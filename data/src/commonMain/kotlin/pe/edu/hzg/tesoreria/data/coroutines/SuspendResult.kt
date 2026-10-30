package pe.edu.hzg.tesoreria.data.coroutines

import kotlinx.coroutines.CancellationException

/** Cancellation belongs to structured concurrency, not to recoverable UI errors. */
internal suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
