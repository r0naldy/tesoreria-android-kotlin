package pe.edu.hzg.tesoreria.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.hzg.tesoreria.data.security.TokenStore
import pe.edu.hzg.tesoreria.data.security.SessionBearerPolicy

class ApiClient(
    baseUrl: String,
    private val tokenStore: TokenStore,
    json: Json,
    engine: HttpClientEngine,
) : AutoCloseable {
    private val bearerPolicy = SessionBearerPolicy(baseUrl, tokenStore)
    private val sessionBearer = createClientPlugin("InstitutionalSessionBearer") {
        onRequest { request, _ ->
            val target = request.url.build().toString()
            check(bearerPolicy.isTrusted(target)) { "Destino fuera del origen HTTPS de la API." }
            val authorization = bearerPolicy.authorizationFor(target, request.headers[HttpHeaders.Authorization])
            request.headers.remove(HttpHeaders.Authorization)
            authorization?.let { request.headers.append(HttpHeaders.Authorization, it) }
        }
        onResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                bearerPolicy.onUnauthorized(response.call.request.url.toString(),
                    response.call.request.headers[HttpHeaders.Authorization])
            }
        }
    }

    val http: HttpClient = HttpClient(engine) {
        expectSuccess = true
        // No credential/body forwarding to a redirect target, and no implicit
        // authentication replay of a mutating request after a 401.
        followRedirects = false

        install(ContentNegotiation) {
            json(json)
        }
        install(sessionBearer)
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 45_000
        }
    }

    override fun close() = http.close()
}
