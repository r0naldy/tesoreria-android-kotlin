package pe.edu.hzg.tesoreria.domain.model

/** Public identity shown before authentication. It never contains credentials or API keys. */
data class InstitutionalIdentity(
    val name: String,
    val city: String,
    val logoDataUrl: String?,
)
