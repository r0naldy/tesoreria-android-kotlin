package pe.edu.hzg.tesoreria.features.academic

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class AndroidAcademicExportManager(private val context: Context) : AcademicExportManager {
    private val directory: File
        get() = File(context.filesDir, "pending-receipts/academic-exports").apply { mkdirs() }

    @Synchronized
    override fun writeBytes(fileName: String, bytes: ByteArray, mimeType: String, title: String): AcademicShareFile {
        AcademicExportRetention.cleanup(directory)
        val file = nextAvailableFile(safeName(fileName))
        file.writeBytes(bytes)
        return AcademicShareFile(uriFor(file).toString(), mimeType, title)
    }

    override fun writeText(fileName: String, text: String, mimeType: String, title: String): AcademicShareFile =
        writeBytes(fileName, text.toByteArray(Charsets.UTF_8), mimeType, title)

    private fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun safeName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(120)
        .ifBlank { "archivo" }

    private fun nextAvailableFile(name: String): File {
        val first = File(directory, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 2
        while (index < 10_000) {
            val candidate = File(directory, "${stem}_$index$extension")
            if (!candidate.exists()) return candidate
            index++
        }
        return File(directory, "${stem}_${System.currentTimeMillis()}$extension")
    }
}

/**
 * Shared files cannot be deleted immediately because the receiving app reads the FileProvider URI
 * asynchronously. A 24-hour window keeps sharing reliable while preventing academic exports from
 * accumulating indefinitely in private storage.
 */
internal object AcademicExportRetention {
    private const val MAX_AGE_MILLIS = 24L * 60L * 60L * 1_000L
    private const val ACTIVE_SHARE_GRACE_MILLIS = 60L * 60L * 1_000L
    private const val MAX_RETAINED_FILES = 12

    fun cleanup(directory: File, nowMillis: Long = System.currentTimeMillis()) {
        val files = directory.listFiles()?.filter(File::isFile).orEmpty()
        files.filter { file ->
            file.name.endsWith(".part") ||
                file.lastModified() <= 0L ||
                nowMillis - file.lastModified() > MAX_AGE_MILLIS
        }.forEach { it.delete() }

        directory.listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            ?.drop(MAX_RETAINED_FILES - 1) // Leave room for the export about to be written.
            // Do not invalidate a URI that may still be open in the Android share sheet.
            ?.filter { nowMillis - it.lastModified() > ACTIVE_SHARE_GRACE_MILLIS }
            ?.forEach { it.delete() }
    }
}
