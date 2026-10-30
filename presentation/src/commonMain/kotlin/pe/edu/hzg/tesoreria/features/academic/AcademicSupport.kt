package pe.edu.hzg.tesoreria.features.academic

data class AcademicShareFile(val uri: String, val mimeType: String, val title: String)

interface AcademicExportManager {
    fun writeBytes(fileName: String, bytes: ByteArray, mimeType: String, title: String): AcademicShareFile
    fun writeText(fileName: String, text: String, mimeType: String, title: String): AcademicShareFile
}
