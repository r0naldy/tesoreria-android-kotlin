package pe.edu.hzg.tesoreria.admin.data

import kotlinx.serialization.Serializable
import pe.edu.hzg.tesoreria.admin.model.ConfiguracionInstitucional
import pe.edu.hzg.tesoreria.admin.model.RegistroAuditoria
import pe.edu.hzg.tesoreria.admin.model.RolUsuario
import pe.edu.hzg.tesoreria.admin.model.UsuarioAdministrado

@Serializable
data class UsuarioAdminDto(
    val id: Int,
    val username: String,
    val nombreCompleto: String,
    val rol: String,
    val estado: String,
    val fechaCreacion: String,
) {
    fun toDomain() = UsuarioAdministrado(
        id = id,
        username = username,
        nombreCompleto = nombreCompleto,
        rol = RolUsuario.fromApi(rol),
        estado = estado,
        fechaCreacion = fechaCreacion,
    )
}

@Serializable
data class CrearUsuarioRequest(
    val username: String,
    val nombreCompleto: String,
    val password: String,
    val rol: String,
)

@Serializable
data class CambiarRolRequest(val rol: String)

@Serializable
data class EditarUsuarioRequest(val id: Int, val nombreCompleto: String)

@Serializable
data class IdCreadoResponse(val id: Int)

@Serializable
data class ConfiguracionAdminDto(
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
) {
    fun toDomain() = ConfiguracionInstitucional(
        id = id,
        nombreInstitucion = nombreInstitucion,
        ciudad = ciudad,
        codigoModular = codigoModular,
        direccion = direccion,
        baseLegal = baseLegal,
        tituloComprobante = tituloComprobante,
        tipoComprobante = tipoComprobante,
        dreGre = dreGre,
        ugel = ugel,
        resolucionCreacion = resolucionCreacion,
        resolucionAutorizacion = resolucionAutorizacion,
        periodoLectivo = periodoLectivo,
        modalidadServicio = modalidadServicio,
        nivelFormativo = nivelFormativo,
        tipoPlan = tipoPlan,
        logoBase64 = logoBase64,
    )
}

@Serializable
data class ActualizarConfiguracionRequest(
    val nombreInstitucion: String,
    val ciudad: String,
    val codigoModular: String,
    val direccion: String,
    val baseLegal: String,
    val tituloComprobante: String,
    val tipoComprobante: String,
    val logoBase64: String?,
    val dreGre: String?,
    val ugel: String?,
    val resolucionCreacion: String?,
    val resolucionAutorizacion: String?,
    val periodoLectivo: String?,
    val modalidadServicio: String?,
    val nivelFormativo: String?,
    val tipoPlan: String?,
) {
    companion object {
        fun from(model: ConfiguracionInstitucional) = ActualizarConfiguracionRequest(
            nombreInstitucion = model.nombreInstitucion,
            ciudad = model.ciudad,
            codigoModular = model.codigoModular,
            direccion = model.direccion,
            baseLegal = model.baseLegal,
            tituloComprobante = model.tituloComprobante,
            tipoComprobante = model.tipoComprobante,
            logoBase64 = model.logoBase64,
            dreGre = model.dreGre,
            ugel = model.ugel,
            resolucionCreacion = model.resolucionCreacion,
            resolucionAutorizacion = model.resolucionAutorizacion,
            periodoLectivo = model.periodoLectivo,
            modalidadServicio = model.modalidadServicio,
            nivelFormativo = model.nivelFormativo,
            tipoPlan = model.tipoPlan,
        )
    }
}

@Serializable
data class RegistroAuditoriaDto(
    val id: Int,
    val usuario: String,
    val fecha: String,
    val ip: String,
    val accion: String,
    val detalle: String? = null,
) {
    fun toDomain() = RegistroAuditoria(id, usuario, fecha, ip, accion, detalle)
}
