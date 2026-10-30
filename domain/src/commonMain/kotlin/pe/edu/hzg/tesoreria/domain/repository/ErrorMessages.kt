package pe.edu.hzg.tesoreria.domain.repository

interface ErrorMessages {
    suspend fun academic(error: Throwable): String
    fun treasury(error: Throwable): String
}
