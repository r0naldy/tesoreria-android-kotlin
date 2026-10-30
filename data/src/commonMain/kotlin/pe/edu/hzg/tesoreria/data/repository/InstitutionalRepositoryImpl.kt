package pe.edu.hzg.tesoreria.data.repository

import kotlinx.coroutines.CancellationException
import pe.edu.hzg.tesoreria.data.remote.TesoreriaApi
import pe.edu.hzg.tesoreria.domain.model.InstitutionalIdentity
import pe.edu.hzg.tesoreria.domain.repository.InstitutionalRepository

class InstitutionalRepositoryImpl(
    private val api: TesoreriaApi,
) : InstitutionalRepository {
    override suspend fun getIdentity(): Result<InstitutionalIdentity> = try {
        val configuration = api.getPublicConfiguration()
        Result.success(
            InstitutionalIdentity(
                name = configuration.nombreInstitucion.trim(),
                city = configuration.ciudad.trim(),
                logoDataUrl = configuration.logoBase64?.takeIf(String::isNotBlank),
            ),
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (cause: Throwable) {
        // The public identity decorates the login. A temporary failure must never prevent access.
        Result.failure(cause)
    }
}
