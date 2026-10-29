package pe.edu.hzg.tesoreria.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.hzg.tesoreria.domain.model.Comprobante
import pe.edu.hzg.tesoreria.domain.model.ComprobanteDraft
import pe.edu.hzg.tesoreria.domain.repository.ComprobanteRepository
import pe.edu.hzg.tesoreria.domain.repository.SyncRepository

data class EgresosUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class EgresosViewModel(
    private val repository: ComprobanteRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    val items: StateFlow<List<Comprobante>> = repository.observeComprobantes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pendingCount: StateFlow<Long> = syncRepository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _state = MutableStateFlow(EgresosUiState())
    val state: StateFlow<EgresosUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = EgresosUiState(isLoading = true)
            repository.refresh()
                .onSuccess { _state.value = EgresosUiState() }
                .onFailure { _state.value = EgresosUiState(message = it.message, isError = true) }
        }
    }

    fun queuePhoto(uri: String) {
        viewModelScope.launch {
            _state.value = EgresosUiState(isLoading = true)
            repository.queueForUpload(ComprobanteDraft(imagenUri = uri))
                .onSuccess {
                    _state.value = EgresosUiState(message = "Comprobante guardado para sincronizar.")
                    synchronize()
                }
                .onFailure { _state.value = EgresosUiState(message = it.message, isError = true) }
        }
    }

    fun synchronize() {
        viewModelScope.launch {
            _state.value = EgresosUiState(isLoading = true)
            syncRepository.synchronize()
                .onSuccess { summary ->
                    _state.value = EgresosUiState(
                        message = "Sincronización: ${summary.succeeded} enviados, ${summary.failed} con error.",
                    )
                }
                .onFailure { _state.value = EgresosUiState(message = it.message, isError = true) }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
