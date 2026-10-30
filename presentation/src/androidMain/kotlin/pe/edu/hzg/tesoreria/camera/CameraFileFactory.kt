package pe.edu.hzg.tesoreria.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraFileFactory {
    fun createImageUri(context: Context): Uri {
        // These files are the offline outbox. Android may evict cache files at any
        // moment, so keep them in app-private storage until the backend confirms
        // the upload. OfflineSyncRepository removes the file after that response.
        val receiptDirectory = File(context.filesDir, PENDING_RECEIPTS_DIRECTORY).apply {
            mkdirs()
        }
        val image = File.createTempFile(
            "receipt_",
            ".jpg",
            receiptDirectory,
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            image,
        )
    }

    const val PENDING_RECEIPTS_DIRECTORY = "pending-receipts"
}
