package pe.edu.hzg.tesoreria.data.sync

import java.net.*

internal actual object ComprobanteUploadRetryPolicy {
    actual fun isDefinitelyBeforeSend(error: Throwable): Boolean = error.causes().any { cause ->
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
