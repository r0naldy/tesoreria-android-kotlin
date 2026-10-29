package pe.edu.hzg.tesoreria.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.domain.model.Programa
import pe.edu.hzg.tesoreria.domain.repository.ProgramaRepository

data class CatalogUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val mutationRevision: Int = 0,
    val mutationError: String? = null,
    val message: String? = null,
    val isError: Boolean = false,
)

class ProgramasViewModel(
    private val repository: ProgramaRepository,
) : ViewModel() {
    val items: StateFlow<List<Programa>> = repository.observeProgramas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (_state.value.isLoading || _state.value.isSaving) return
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            repository.refresh()
                .onSuccess { _state.value = _state.value.copy(isLoading = false) }
                .onFailure { _state.value = _state.value.copy(isLoading = false, message = it.message, isError = true) }
        }
    }
    fun create(nombre: String) = executeMutation { repository.create(nombre).map { Unit } }
    fun update(item: Programa, nombre: String, activo: Boolean) = executeMutation {
        repository.update(item.copy(nombre = nombre, estado = if (activo) "Activo" else "Inactivo"))
    }
    fun delete(id: Int) = executeMutation("Programa eliminado del servidor.") { repository.delete(id) }
    fun discardPending(id: Int) = executeMutation("Borrador local descartado. Se recuperó el catálogo del servidor; no se eliminó ningún programa remoto.") { repository.discardPending(id) }
    fun clearMessage() { _state.value = _state.value.copy(message = null, mutationError = null, isError = false) }

    private fun executeMutation(successMessage: String = "Programa guardado en el servidor.", block: suspend () -> Result<Unit>) {
        if (_state.value.isSaving) return
        _state.value = _state.value.copy(isSaving = true, mutationError = null, message = null, isError = false)
        viewModelScope.launch {
            block()
                .onSuccess { _state.value = _state.value.copy(isSaving = false, mutationRevision = _state.value.mutationRevision + 1, message = successMessage, isError = false) }
                .onFailure { error ->
                    val message = error.message ?: "No se pudo guardar el programa."
                    _state.value = _state.value.copy(isSaving = false, message = message, mutationError = message, isError = true)
                }
        }
    }
}
