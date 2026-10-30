package pe.edu.hzg.tesoreria.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.admin.domain.AdminRepository
import pe.edu.hzg.tesoreria.admin.model.CampoConfiguracion
import pe.edu.hzg.tesoreria.admin.model.ConfiguracionInstitucional
import pe.edu.hzg.tesoreria.admin.model.NuevoUsuario
import pe.edu.hzg.tesoreria.admin.model.RegistroAuditoria
import pe.edu.hzg.tesoreria.admin.model.RolUsuario
import pe.edu.hzg.tesoreria.admin.model.UsuarioAdministrado

data class UsuariosUiState(
    val usuarios: List<UsuarioAdministrado> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val mutationRevision: Long = 0,
    val mutationError: String? = null,
)

class UsuariosViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(UsuariosUiState())
    val state: StateFlow<UsuariosUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.listarUsuarios().fold(
                onSuccess = { usuarios ->
                    _state.update { it.copy(usuarios = usuarios, isLoading = false, isError = false) }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(isLoading = false, message = failure.userMessage(), isError = true)
                    }
                },
            )
        }
    }

    fun crear(username: String, nombre: String, password: String, rol: RolUsuario) {
        val validation = when {
            username.isBlank() -> "El usuario es obligatorio."
            username.trim().length > 50 -> "El usuario no debe superar 50 caracteres."
            nombre.isBlank() -> "El nombre completo es obligatorio."
            nombre.trim().length > 150 -> "El nombre no debe superar 150 caracteres."
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
            else -> null
        }
        if (validation != null) {
            showMessage(validation, isError = true)
            return
        }
        mutate("Usuario creado correctamente.") {
            repository.crearUsuario(NuevoUsuario(username, nombre, password, rol))
        }
    }

    fun alternarEstado(usuario: UsuarioAdministrado, currentUsername: String) {
        if (usuario.username.equals(currentUsername, ignoreCase = true)) {
            showMessage("No puedes desactivar tu propia cuenta desde la aplicación.", isError = true)
            return
        }
        val activeAdmins = _state.value.usuarios.count { it.activo && it.rol == RolUsuario.Administrador }
        if (usuario.activo && usuario.rol == RolUsuario.Administrador && activeAdmins <= 1) {
            showMessage("Debe quedar al menos un Administrador activo.", isError = true)
            return
        }
        mutate(if (usuario.activo) "Usuario desactivado." else "Usuario activado.") {
            repository.alternarEstadoUsuario(usuario.id)
        }
    }

    fun cambiarRol(usuario: UsuarioAdministrado, rol: RolUsuario, currentUsername: String) {
        if (usuario.username.equals(currentUsername, ignoreCase = true)) {
            showMessage("No puedes cambiar tu propio rol.", isError = true)
            return
        }
        if (usuario.rol == rol) return
        mutate("Rol actualizado. Se aplicará en el próximo ingreso del usuario.") {
            repository.cambiarRolUsuario(usuario.id, rol)
        }
    }

    fun eliminar(usuario: UsuarioAdministrado, currentUsername: String) {
        if (usuario.username.equals(currentUsername, ignoreCase = true)) {
            showMessage("No puedes eliminar tu propia cuenta.", isError = true)
            return
        }
        mutate("Usuario eliminado.") { repository.eliminarUsuario(usuario.id) }
    }

    fun editar(usuario: UsuarioAdministrado, nombre: String) {
        if (nombre.isBlank() || nombre.trim().length > 150) {
            showMessage("El nombre completo es obligatorio y admite hasta 150 caracteres.", isError = true)
            return
        }
        mutate("Nombre actualizado. La identidad de la sesión se actualizará en el próximo ingreso.") {
            repository.editarUsuario(usuario.id, nombre)
        }
    }

    fun clearMutationError() = _state.update { it.copy(mutationError = null) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun mutate(successMessage: String, operation: suspend () -> Result<Unit>) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, message = null, mutationError = null) }
        viewModelScope.launch {
            operation().fold(
                onSuccess = {
                    repository.listarUsuarios().fold(
                        onSuccess = { usuarios ->
                            _state.update {
                                it.copy(
                                    usuarios = usuarios,
                                    isSaving = false,
                                    message = successMessage,
                                    isError = false,
                                    mutationRevision = it.mutationRevision + 1,
                                )
                            }
                        },
                        onFailure = { failure ->
                            _state.update {
                                it.copy(
                                    isSaving = false,
                                    message = "$successMessage No se pudo actualizar la lista: ${failure.userMessage()}",
                                    isError = false,
                                    mutationRevision = it.mutationRevision + 1,
                                )
                            }
                        },
                    )
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(isSaving = false, message = failure.userMessage(), isError = true, mutationError = failure.userMessage())
                    }
                },
            )
        }
    }

    private fun showMessage(message: String, isError: Boolean) {
        _state.update { it.copy(message = message, isError = isError, mutationError = if (isError) message else null) }
    }
}

data class ConfiguracionUiState(
    val form: ConfiguracionInstitucional = ConfiguracionInstitucional(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loaded: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class ConfiguracionViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ConfiguracionUiState())
    val state: StateFlow<ConfiguracionUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.obtenerConfiguracion().fold(
                onSuccess = { form ->
                    _state.update {
                        it.copy(form = form, isLoading = false, loaded = true, isError = false)
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(isLoading = false, message = failure.userMessage(), isError = true)
                    }
                },
            )
        }
    }

    fun update(campo: CampoConfiguracion, value: String) {
        _state.update { state ->
            val form = state.form
            state.copy(
                form = when (campo) {
                    CampoConfiguracion.NombreInstitucion -> form.copy(nombreInstitucion = value)
                    CampoConfiguracion.Ciudad -> form.copy(ciudad = value)
                    CampoConfiguracion.CodigoModular -> form.copy(codigoModular = value)
                    CampoConfiguracion.Direccion -> form.copy(direccion = value)
                    CampoConfiguracion.BaseLegal -> form.copy(baseLegal = value)
                    CampoConfiguracion.TituloComprobante -> form.copy(tituloComprobante = value)
                    CampoConfiguracion.TipoComprobante -> form.copy(tipoComprobante = value)
                    CampoConfiguracion.DreGre -> form.copy(dreGre = value)
                    CampoConfiguracion.Ugel -> form.copy(ugel = value)
                    CampoConfiguracion.ResolucionCreacion -> form.copy(resolucionCreacion = value)
                    CampoConfiguracion.ResolucionAutorizacion -> form.copy(resolucionAutorizacion = value)
                    CampoConfiguracion.PeriodoLectivo -> form.copy(periodoLectivo = value)
                    CampoConfiguracion.ModalidadServicio -> form.copy(modalidadServicio = value)
                    CampoConfiguracion.NivelFormativo -> form.copy(nivelFormativo = value)
                    CampoConfiguracion.TipoPlan -> form.copy(tipoPlan = value)
                },
                message = null,
            )
        }
    }

    fun updateLogo(dataUrl: String) {
        _state.update { it.copy(form = it.form.copy(logoBase64 = dataUrl), message = null) }
    }

    fun reportLogoError(message: String) {
        _state.update { it.copy(message = message, isError = true) }
    }

    fun save(canEdit: Boolean) {
        if (!canEdit) {
            _state.update { it.copy(message = "Solo un Administrador puede guardar cambios.", isError = true) }
            return
        }
        if (_state.value.form.nombreInstitucion.isBlank()) {
            _state.update { it.copy(message = "El nombre de la institución es obligatorio.", isError = true) }
            return
        }
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            repository.actualizarConfiguracion(_state.value.form).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isSaving = false, message = "Configuración actualizada.", isError = false)
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(isSaving = false, message = failure.userMessage(), isError = true)
                    }
                },
            )
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class AuditoriaUiState(
    val registros: List<RegistroAuditoria> = emptyList(),
    val top: Int = 200,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class AuditoriaViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AuditoriaUiState())
    val state: StateFlow<AuditoriaUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setTop(top: Int) {
        _state.update { it.copy(top = top.coerceIn(1, 500)) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            repository.listarAuditoria(_state.value.top).fold(
                onSuccess = { registros ->
                    _state.update { it.copy(registros = registros, isLoading = false) }
                },
                onFailure = { failure ->
                    _state.update { it.copy(isLoading = false, message = failure.userMessage()) }
                },
            )
        }
    }
}

private fun Throwable.userMessage(): String =
    message?.takeIf(String::isNotBlank) ?: "Ocurrió un error inesperado."
