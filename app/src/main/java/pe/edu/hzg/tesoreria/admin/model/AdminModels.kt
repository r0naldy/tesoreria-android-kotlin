package pe.edu.hzg.tesoreria.admin.model

enum class RolUsuario(val apiValue: String) {
    Administrador("Administrador"),
    Finanzas("Finanzas"),
    ;

    companion object {
        fun fromApi(value: String): RolUsuario =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: Finanzas
    }
}

data class UsuarioAdministrado(
    val id: Int,
    val username: String,
    val nombreCompleto: String,
    val rol: RolUsuario,
    val estado: String,
    val fechaCreacion: String,
) {
    val activo: Boolean get() = estado.equals("Activo", ignoreCase = true)
}

data class NuevoUsuario(
    val username: String,
    val nombreCompleto: String,
    val password: String,
    val rol: RolUsuario,
)

data class ConfiguracionInstitucional(
    val id: Int = 0,
    val nombreInstitucion: String = "",
    val ciudad: String = "",
    val codigoModular: String = "",
    val direccion: String = "",
    val baseLegal: String = "",
    val tituloComprobante: String = "",
    val tipoComprobante: String = "",
    val dreGre: String = "",
    val ugel: String = "",
    val resolucionCreacion: String = "",
    val resolucionAutorizacion: String = "",
    val periodoLectivo: String = "",
    val modalidadServicio: String = "",
    val nivelFormativo: String = "",
    val tipoPlan: String = "",
    val logoBase64: String? = null,
)

enum class CampoConfiguracion {
    NombreInstitucion,
    Ciudad,
    CodigoModular,
    Direccion,
    BaseLegal,
    TituloComprobante,
    TipoComprobante,
    DreGre,
    Ugel,
    ResolucionCreacion,
    ResolucionAutorizacion,
    PeriodoLectivo,
    ModalidadServicio,
    NivelFormativo,
    TipoPlan,
}

data class RegistroAuditoria(
    val id: Int,
    val usuario: String,
    val fecha: String,
    val ip: String,
    val accion: String,
    val detalle: String?,
)
