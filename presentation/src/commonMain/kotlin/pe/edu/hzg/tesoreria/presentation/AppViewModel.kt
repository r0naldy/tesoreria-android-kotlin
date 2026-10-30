package pe.edu.hzg.tesoreria.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.domain.model.SessionIdentity
import pe.edu.hzg.tesoreria.domain.repository.SyncScheduler
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.domain.repository.AuthRepository

sealed interface SsoLoginUiState {
    data object Idle : SsoLoginUiState
    data object Loading : SsoLoginUiState
    data class Error(val message: String) : SsoLoginUiState
}

class AppViewModel(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    private val _ssoLoginState = MutableStateFlow<SsoLoginUiState>(SsoLoginUiState.Idle)
    val ssoLoginState = _ssoLoginState.asStateFlow()
    private var ssoInProgress = false

    val session: StateFlow<AuthSession?> = authRepository.observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // A queue abandoned by logout remains intact. When its owner returns,
        // WorkManager gets another chance to process only that identity's rows.
        viewModelScope.launch {
            session
                .map { it?.let(SessionIdentity::fromSession) }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { syncScheduler.schedule() }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    /** Exchanges the short-lived ASISTO token for the normal Tesorería JWT. */
    fun loginFromAsisto(token: String) {
        if (ssoInProgress) return
        ssoInProgress = true
        _ssoLoginState.value = SsoLoginUiState.Loading
        viewModelScope.launch {
            authRepository.loginFromAsisto(token)
                .onSuccess { _ssoLoginState.value = SsoLoginUiState.Idle }
                .onFailure {
                    _ssoLoginState.value = SsoLoginUiState.Error(
                        "No se pudo entrar desde ASISTO. El acceso puede haber caducado o ya fue utilizado; vuelve a ASISTO y solicita uno nuevo.",
                    )
                }
            ssoInProgress = false
        }
    }

    fun showInvalidSsoLink(message: String) {
        if (!ssoInProgress) _ssoLoginState.value = SsoLoginUiState.Error(message)
    }

    fun dismissSsoMessage() {
        if (!ssoInProgress) _ssoLoginState.value = SsoLoginUiState.Idle
    }
}
