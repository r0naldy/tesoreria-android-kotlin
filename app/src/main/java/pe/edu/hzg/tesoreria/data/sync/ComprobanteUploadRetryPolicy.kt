package pe.edu.hzg.tesoreria.data.sync

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException

/**
 * A receipt POST has financial side effects and the backend currently has no
 * idempotency key. Only failures that prove a connection was never established
 * are eligible for automatic retry. Timeouts, HTTP responses, broken response
 * bodies and all unknown I/O failures are deliberately treated as ambiguous.
 */
internal object ComprobanteUploadRetryPolicy {
    fun isDefinitelyBeforeSend(error: Throwable): Boolean = error.causes().any { cause ->
        cause is UnknownHostException ||
            cause is ConnectException ||
            cause is NoRouteToHostException ||
            cause::class.qualifiedName in SAFE_KTOR_FAILURES
    }

    private fun Throwable.causes(): Sequence<Throwable> =
        generateSequence(this) { current -> current.cause?.takeUnless { it === current } }

    private val SAFE_KTOR_FAILURES = setOf(
        "io.ktor.util.network.UnresolvedAddressException",
        "io.ktor.client.network.sockets.ConnectTimeoutException",
    )
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
