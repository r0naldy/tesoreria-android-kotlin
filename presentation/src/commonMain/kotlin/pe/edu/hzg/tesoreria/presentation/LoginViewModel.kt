package pe.edu.hzg.tesoreria.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.domain.repository.AuthRepository
import pe.edu.hzg.tesoreria.domain.repository.InstitutionalRepository

private const val DEFAULT_INSTITUTION_NAME = "CETPRO Horacio Zeballos Gámez"

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val institutionName: String = DEFAULT_INSTITUTION_NAME,
    val institutionCity: String = "",
    val institutionLogoDataUrl: String? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val institutionalRepository: InstitutionalRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            institutionalRepository.getIdentity().onSuccess { identity ->
                _state.update {
                    it.copy(
                        institutionName = identity.name.ifBlank { DEFAULT_INSTITUTION_NAME },
                        institutionCity = identity.city,
                        institutionLogoDataUrl = identity.logoDataUrl,
                    )
                }
            }
        }
    }

    fun onUsernameChanged(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value, error = null) }

    fun login() {
        val current = _state.value
        if (current.isLoading) return
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "Ingrese usuario y contraseña.") }
            return
        }
        val username = current.username.trim()
        val password = current.password

        viewModelScope.launch {
            // Do not retain the secret in observable UI state while the network request runs.
            _state.update { it.copy(password = "", isLoading = true, error = null) }
            authRepository.login(username, password)
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            password = "",
                            isLoading = false,
                            error = error.message ?: "No se pudo iniciar sesión.",
                        )
                    }
                }
                // The ViewModel is scoped to the Activity and survives the authenticated
                // composition. Reset the whole form so a later logout can never reveal the
                // credentials that were used to enter the app.
                .onSuccess {
                    _state.update {
                        it.copy(username = "", password = "", isLoading = false, error = null)
                    }
                }
        }
    }
}
