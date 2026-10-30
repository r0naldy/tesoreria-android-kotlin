package pe.edu.hzg.tesoreria.data.sync

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprobanteUploadRetryPolicyTest {
    @Test
    fun `cancelled OCR request remains a cancellation rather than a retryable failure`() = runBlocking {
        val cancelled = CancellationException("worker stopped")
        val failure = runCatching { executeComprobantePost<Unit> { throw cancelled } }.exceptionOrNull()
        assertSame(cancelled, failure)
    }

    @Test
    fun `ambiguous OCR response is wrapped with retry disabled`() = runBlocking {
        val cause = SocketTimeoutException("response lost")
        val failure = runCatching { executeComprobantePost<Unit> { throw cause } }.exceptionOrNull()
        assertTrue(failure is ComprobantePostFailure)
        assertFalse((failure as ComprobantePostFailure).retryIsSafe)
        assertSame(cause, failure.cause)
    }

    @Test
    fun `definitely unsent OCR request is eligible for deferred retry`() = runBlocking {
        val cause = UnknownHostException()
        val failure = runCatching { executeComprobantePost<Unit> { throw cause } }.exceptionOrNull()
        assertTrue(failure is ComprobantePostFailure)
        assertTrue((failure as ComprobantePostFailure).retryIsSafe)
    }

    @Test
    fun `confirmed OCR response passes through without another attempt`() = runBlocking {
        var calls = 0
        assertEquals(42, executeComprobantePost { calls++; 42 })
        assertEquals(1, calls)
    }

    @Test
    fun `dns and connection establishment failures are safe to retry`() {
        assertTrue(ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(UnknownHostException()))
        assertTrue(ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(ConnectException()))
        assertTrue(
            ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(
                IllegalStateException("wrapper", UnknownHostException()),
            ),
        )
    }

    @Test
    fun `timeouts and unknown response failures are never automatically retried`() {
        assertFalse(ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(SocketTimeoutException()))
        assertFalse(ComprobanteUploadRetryPolicy.isDefinitelyBeforeSend(IllegalStateException("bad response")))
    }
}
