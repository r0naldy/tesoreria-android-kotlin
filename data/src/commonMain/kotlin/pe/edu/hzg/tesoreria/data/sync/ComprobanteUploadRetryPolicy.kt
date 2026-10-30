package pe.edu.hzg.tesoreria.data.sync

import kotlinx.coroutines.CancellationException

/**
 * A receipt POST has financial side effects and the backend currently has no
 * idempotency key. Only failures that prove a connection was never established
 * are eligible for automatic retry. Timeouts, HTTP responses, broken response
 * bodies and all unknown I/O failures are deliberately treated as ambiguous.
 */
internal expect object ComprobanteUploadRetryPolicy {
    fun isDefinitelyBeforeSend(error: Throwable): Boolean
}

/** Applies the same no-replay/cancellation boundary to every receipt POST. */
internal suspend fun <T> executeComprobantePost(block: suspend () -> T): T = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    throw ComprobantePostFailure(
        retryIsSafe = ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(error),
        cause = error,
    )
}

internal class ComprobantePostFailure(
    val retryIsSafe: Boolean,
    cause: Throwable,
) : Exception(cause.message ?: "No se pudo confirmar el envío del comprobante.", cause)

internal class ComprobantePostPersistFailure(cause: Throwable) :
    Exception("El servidor respondió, pero no se pudo confirmar el resultado localmente.", cause)
