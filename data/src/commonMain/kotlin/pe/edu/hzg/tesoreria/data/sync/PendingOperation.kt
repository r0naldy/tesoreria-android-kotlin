package pe.edu.hzg.tesoreria.data.sync

data class PendingOperation(
    val id: String,
    val ownerIdentity: String,
    val entityType: String,
    val entityLocalId: String,
    val operation: String,
    val payloadJson: String,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
    val nextAttemptAt: Long,
)

object PendingEntityType {
    const val PROGRAMA = "programa"
    const val SERVICIO = "servicio"
    const val COMPROBANTE = "comprobante"
}

object PendingOperationType {
    const val CREATE = "create"
    const val UPDATE = "update"
    const val DELETE = "delete"
    const val UPLOAD = "upload"
}
