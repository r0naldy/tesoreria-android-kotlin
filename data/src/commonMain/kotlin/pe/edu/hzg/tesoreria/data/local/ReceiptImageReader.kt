package pe.edu.hzg.tesoreria.data.local

data class ReceiptImage(
    val bytes: ByteArray,
    val fileName: String,
    val mediaType: String,
)

interface ReceiptImageReader {
    fun read(uriText: String): ReceiptImage
    fun deleteAfterConfirmedUpload(uriText: String): Boolean
}
