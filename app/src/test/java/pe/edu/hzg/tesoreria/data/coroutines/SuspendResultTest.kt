package pe.edu.hzg.tesoreria.data.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SuspendResultTest {
    @Test fun `successful result keeps its value`() = runBlocking {
        assertEquals("done", runSuspendCatching { "done" }.getOrThrow())
    }

    @Test fun `ordinary exceptions are returned unchanged`() = runBlocking {
        val cause = IllegalStateException("validation failed")
        val result = runSuspendCatching<Unit> { throw cause }
        assertSame(cause, result.exceptionOrNull())
    }

    @Test fun `cancellation propagates instead of becoming a recoverable failure`() = runBlocking {
        val cancelled = CancellationException("screen closed")
        val thrown = runCatching { runSuspendCatching<Unit> { throw cancelled } }.exceptionOrNull()
        assertSame(cancelled, thrown)
    }

    @Test fun `nested operation cannot swallow cancellation`() = runBlocking {
        val cancelled = CancellationException("worker stopped")
        val thrown = runCatching {
            runSuspendCatching { runSuspendCatching<Unit> { throw cancelled } }
        }.exceptionOrNull()
        assertSame(cancelled, thrown)
    }

    @Test fun `fatal errors are not presented as ordinary business errors`() = runBlocking {
        val fatal = AssertionError("unexpected fatal condition")
        val thrown = runCatching { runSuspendCatching<Unit> { throw fatal } }.exceptionOrNull()
        assertSame(fatal, thrown)
    }
}
