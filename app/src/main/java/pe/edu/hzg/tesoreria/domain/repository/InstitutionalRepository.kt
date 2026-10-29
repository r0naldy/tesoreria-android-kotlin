package pe.edu.hzg.tesoreria.domain.repository

import pe.edu.hzg.tesoreria.domain.model.InstitutionalIdentity

interface InstitutionalRepository {
    /** Reads the anonymous /api/configuracion projection used by every login surface. */
    suspend fun getIdentity(): Result<InstitutionalIdentity>
}
