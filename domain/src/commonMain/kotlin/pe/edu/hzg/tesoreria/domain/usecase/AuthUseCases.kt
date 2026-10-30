package pe.edu.hzg.tesoreria.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): Result<AuthSession> =
        repository.login(username.trim(), password)
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}

class ObserveSessionUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<AuthSession?> = repository.observeSession()
}
