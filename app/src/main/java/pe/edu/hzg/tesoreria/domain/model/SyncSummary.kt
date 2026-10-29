package pe.edu.hzg.tesoreria.domain.model

data class SyncSummary(
    val processed: Int,
    val succeeded: Int,
    val failed: Int,
)
