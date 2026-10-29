package pe.edu.hzg.tesoreria.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.hzg.tesoreria.domain.model.AuthSession

class KeystoreTokenStore(
    context: Context,
    private val json: Json,
) : TokenStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val state = MutableStateFlow(readStoredSession())

    override fun observe(): Flow<AuthSession?> = state.asStateFlow()

    override suspend fun read(): AuthSession? {
        val session = state.value ?: return null
        if (session.isExpired()) {
            clearIfTokenMatches(session.token)
            return null
        }
        return session
    }

    override suspend fun save(session: AuthSession) {
        mutex.withLock {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val plainText = json.encodeToString<StoredSession>(StoredSession.from(session)).encodeToByteArray()
            val encrypted = cipher.doFinal(plainText)

            preferences.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
            state.value = session
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            clearLocked()
        }
    }

    override suspend fun clearIfTokenMatches(token: String): Boolean = mutex.withLock {
        if (state.value?.token != token) return@withLock false
        clearLocked()
        true
    }

    private fun clearLocked() {
        preferences.edit().clear().apply()
        state.value = null
    }

    private fun readStoredSession(): AuthSession? = runCatching {
        val encodedCipherText = preferences.getString(KEY_CIPHERTEXT, null) ?: return@runCatching null
        val encodedIv = preferences.getString(KEY_IV, null) ?: return@runCatching null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(encodedIv, Base64.NO_WRAP)),
        )
        val plainText = cipher.doFinal(Base64.decode(encodedCipherText, Base64.NO_WRAP)).decodeToString()
        json.decodeFromString<StoredSession>(plainText).toDomain().takeUnless { session ->
            session.isExpired().also { expired ->
                if (expired) preferences.edit().clear().apply()
            }
        }
    }.getOrElse {
        preferences.edit().clear().apply()
        null
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    @Serializable
    private data class StoredSession(
        val token: String,
        val username: String,
        val nombreCompleto: String,
        val rol: String,
        val expiresAtEpochMillis: Long,
    ) {
        fun toDomain() = AuthSession(token, username, nombreCompleto, rol, expiresAtEpochMillis)

        companion object {
            fun from(session: AuthSession) = StoredSession(
                token = session.token,
                username = session.username,
                nombreCompleto = session.nombreCompleto,
                rol = session.rol,
                expiresAtEpochMillis = session.expiresAtEpochMillis,
            )
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFERENCES = "secure_auth_session"
        const val KEY_ALIAS = "tesoreria_jwt_aes"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_IV = "iv"
    }
}
