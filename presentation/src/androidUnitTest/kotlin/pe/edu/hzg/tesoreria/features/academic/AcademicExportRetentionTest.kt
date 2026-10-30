package pe.edu.hzg.tesoreria.features.academic

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AcademicExportRetentionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `deletes expired exports but retains recent files`() {
        val directory = temporaryFolder.newFolder("exports")
        val now = 2_000_000_000L
        val recent = createFile(directory, "recent.csv", now - 1_000L)
        val expired = createFile(directory, "expired.xlsx", now - 25L * 60L * 60L * 1_000L)

        AcademicExportRetention.cleanup(directory, now)

        assertTrue(recent.exists())
        assertFalse(expired.exists())
    }

    @Test
    fun `keeps room for the next export without deleting newest shares`() {
        val directory = temporaryFolder.newFolder("bounded")
        val now = 2_000_000_000L
        repeat(13) { index ->
            createFile(directory, "export-$index.csv", now - 2L * 60L * 60L * 1_000L - index * 1_000L)
        }

        AcademicExportRetention.cleanup(directory, now)

        assertEquals(11, directory.listFiles()?.size)
        assertTrue(File(directory, "export-0.csv").exists())
        assertFalse(File(directory, "export-12.csv").exists())
    }

    @Test
    fun `does not invalidate recent share URIs just to enforce the count cap`() {
        val directory = temporaryFolder.newFolder("active-shares")
        val now = 2_000_000_000L
        repeat(13) { index -> createFile(directory, "active-$index.csv", now - index * 1_000L) }

        AcademicExportRetention.cleanup(directory, now)

        assertEquals(13, directory.listFiles()?.size)
    }

    private fun createFile(directory: File, name: String, modified: Long): File =
        File(directory, name).apply {
            writeText("test")
            check(setLastModified(modified))
        }
}
