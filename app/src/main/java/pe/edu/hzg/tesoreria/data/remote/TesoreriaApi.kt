package pe.edu.hzg.tesoreria.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import pe.edu.hzg.tesoreria.data.remote.dto.AuthResponseDto
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteDto
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteListItemDto
import pe.edu.hzg.tesoreria.data.remote.dto.CreateProgramaRequestDto
import pe.edu.hzg.tesoreria.data.remote.dto.CreateServicioRequestDto
import pe.edu.hzg.tesoreria.data.remote.dto.CreatedIdDto
import pe.edu.hzg.tesoreria.data.remote.dto.LoginRequestDto
import pe.edu.hzg.tesoreria.data.remote.dto.ProgramaDto
import pe.edu.hzg.tesoreria.data.remote.dto.PublicConfigurationDto
import pe.edu.hzg.tesoreria.data.remote.dto.ServicioDto
import pe.edu.hzg.tesoreria.data.remote.dto.SsoLoginRequestDto
import pe.edu.hzg.tesoreria.data.remote.dto.UpdateProgramaRequestDto
import pe.edu.hzg.tesoreria.data.remote.dto.UpdateServicioRequestDto

class TesoreriaApi(
    apiClient: ApiClient,
    baseUrl: String,
) : CatalogApi {
    private val client = apiClient.http
    private val apiRoot = "${baseUrl.trimEnd('/')}/api"

    suspend fun login(username: String, password: String): AuthResponseDto =
        client.post("$apiRoot/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(username, password))
        }.body()

    suspend fun loginFromAsisto(token: String): AuthResponseDto =
        client.post("$apiRoot/auth/sso") {
            contentType(ContentType.Application.Json)
            setBody(SsoLoginRequestDto(token))
        }.body()

    suspend fun getPublicConfiguration(): PublicConfigurationDto =
        client.get("$apiRoot/configuracion").body()

    override suspend fun getProgramas(soloActivos: Boolean, bearerToken: String?): List<ProgramaDto> =
        client.get("$apiRoot/programas") {
            bearerToken?.let { bearerAuth(it) }
            url { parameters.append("soloActivos", soloActivos.toString()) }
        }.body()

    override suspend fun createPrograma(nombre: String, bearerToken: String?): Int =
        client.post("$apiRoot/programas") {
            bearerToken?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(CreateProgramaRequestDto(nombre))
        }.body<CreatedIdDto>().id

    override suspend fun updatePrograma(
        id: Int,
        nombre: String,
        activo: Boolean,
        bearerToken: String?,
    ) {
        client.put("$apiRoot/programas/$id") {
            bearerToken?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(UpdateProgramaRequestDto(id, nombre, activo))
        }
    }

    override suspend fun deletePrograma(id: Int, bearerToken: String?) {
        client.delete("$apiRoot/programas/$id") {
            bearerToken?.let { bearerAuth(it) }
        }
    }

    override suspend fun getServicios(soloActivos: Boolean, bearerToken: String?): List<ServicioDto> =
        client.get("$apiRoot/servicios") {
            bearerToken?.let { bearerAuth(it) }
            url { parameters.append("soloActivos", soloActivos.toString()) }
        }.body()

    override suspend fun createServicio(
        nombre: String,
        precio: Double,
        bearerToken: String?,
    ): Int =
        client.post("$apiRoot/servicios") {
            bearerToken?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(CreateServicioRequestDto(nombre, precio))
        }.body<CreatedIdDto>().id

    override suspend fun updateServicio(
        id: Int,
        nombre: String,
        precio: Double,
        activo: Boolean,
        bearerToken: String?,
    ) {
        client.put("$apiRoot/servicios/$id") {
            bearerToken?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(UpdateServicioRequestDto(id, nombre, precio, activo))
        }
    }

    override suspend fun deleteServicio(id: Int, bearerToken: String?) {
        client.delete("$apiRoot/servicios/$id") {
            bearerToken?.let { bearerAuth(it) }
        }
    }

    suspend fun getComprobantes(): List<ComprobanteListItemDto> =
        client.get("$apiRoot/comprobantes").body()

    suspend fun getComprobante(id: Int): ComprobanteDto =
        client.get("$apiRoot/comprobantes/$id").body()

    suspend fun uploadComprobante(
        bytes: ByteArray,
        fileName: String,
        mediaType: String = ContentType.Image.JPEG.toString(),
        bearerToken: String? = null,
    ): ComprobanteDto = client.post("$apiRoot/comprobantes") {
        bearerToken?.let { bearerAuth(it) }
        setBody(
            MultiPartFormDataContent(
                formData {
                    append(
                        key = "imagen",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, mediaType)
                            append(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.File.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    fileName,
                                ).toString(),
                            )
                        },
                    )
                },
            ),
        )
    }.body()
}
