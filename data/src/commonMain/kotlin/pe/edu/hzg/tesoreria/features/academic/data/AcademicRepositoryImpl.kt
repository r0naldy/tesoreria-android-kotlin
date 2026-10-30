package pe.edu.hzg.tesoreria.features.academic.data

import pe.edu.hzg.tesoreria.features.academic.domain.model.*
import pe.edu.hzg.tesoreria.features.academic.domain.repository.AcademicRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import io.ktor.client.plugins.ClientRequestException

/**
 * Repositorio online-first con respaldo en memoria para lecturas ya vistas.
 * Las mutaciones críticas son llamadas directas: no se encolan, no se repiten
 * automáticamente y el servidor siempre decide si se aplicaron.
 */
class AcademicRepositoryImpl(private val api: AcademicApi) : AcademicRepository {
    private val cacheMutex = Mutex()
    private var studentCache: List<Student> = emptyList()
    private var periodsCache: List<Period> = emptyList()
    private val recordsCache = mutableMapOf<Int?, List<Record>>()
    private var programsCache: List<AcademicProgram> = emptyList()
    private var shiftsCache: List<Shift> = emptyList()
    private var reasonsCache: List<String> = emptyList()
    private val comparisonCache = mutableMapOf<Pair<String?, String?>, Comparison>()

    override suspend fun students(search: String?): List<Student> = onlineFirst(
        remote = {
            api.students(search).also { fresh ->
                if (search.isNullOrBlank()) cacheMutex.withLock { studentCache = fresh }
            }
        },
        cached = {
            val query = search.orEmpty().trim().lowercase()
            cacheMutex.withLock {
                studentCache.filter {
                    query.isBlank() || it.dni.contains(query) ||
                        it.nombreCompleto.lowercase().contains(query)
                }
            }
        },
    )

    override suspend fun lookupDni(dni: String) = api.lookupDni(dni)

    override suspend fun createStudent(request: CreateStudentCommand): Int =
        api.createStudent(request).also { clearStudentsAndRecords() }

    override suspend fun updateStudent(request: UpdateStudentCommand) {
        api.updateStudent(request)
        clearStudentsAndRecords()
    }

    override suspend fun deleteStudent(id: Int) {
        api.deleteStudent(id)
        clearStudentsAndRecords()
    }

    override suspend fun withdrawalReasons(): List<String> = onlineFirst(
        remote = { api.withdrawalReasons().also { cacheMutex.withLock { reasonsCache = it } } },
        cached = { cacheMutex.withLock { reasonsCache } },
    )

    override suspend fun ratifyStudent(id: Int): Ratification =
        api.ratifyStudent(id).also { clearStudentsAndRecords() }

    override suspend fun withdrawStudent(id: Int, request: WithdrawalCommand): WithdrawalResult =
        api.withdrawStudent(id, request).also { clearStudentsAndRecords() }

    override suspend fun programs(): List<AcademicProgram> = onlineFirst(
        remote = { api.programs().also { cacheMutex.withLock { programsCache = it } } },
        cached = { cacheMutex.withLock { programsCache } },
    )

    override suspend fun shifts(): List<Shift> = onlineFirst(
        remote = { api.shifts().also { cacheMutex.withLock { shiftsCache = it } } },
        cached = { cacheMutex.withLock { shiftsCache } },
    )

    override suspend fun periods(): List<Period> = onlineFirst(
        remote = { api.periods().also { cacheMutex.withLock { periodsCache = it } } },
        cached = { cacheMutex.withLock { periodsCache } },
    )

    override suspend fun activePeriod(): Period? = runCatching { api.activePeriod() }
        .getOrElse { error ->
            if (error is CancellationException || error is ClientRequestException) throw error
            cacheMutex.withLock {
                periodsCache.firstOrNull { it.estado.equals("Abierto", ignoreCase = true) }
            } ?: throw error
        }

    override suspend fun periodSummary(id: Int) = api.periodSummary(id)
    override suspend fun periodDetail(id: Int) = api.periodDetail(id)

    override suspend fun openPeriod(request: OpenPeriodCommand): Int =
        api.openPeriod(request).also { cacheMutex.withLock { periodsCache = emptyList() } }

    override suspend fun closePeriod(id: Int): PeriodSummary =
        api.closePeriod(id).also { cacheMutex.withLock { periodsCache = emptyList() } }

    override suspend fun records(periodId: Int?): List<Record> = onlineFirst(
        remote = { api.records(periodId).also { cacheMutex.withLock { recordsCache[periodId] = it } } },
        cached = { cacheMutex.withLock { recordsCache[periodId].orEmpty() } },
    )

    override suspend fun roster(id: Int, semester: String?) = api.roster(id, semester)
    override suspend fun recordExcel(id: Int, semester: String?) = api.recordExcel(id, semester)

    override suspend fun createRecord(request: CreateRecordCommand): Int =
        api.createRecord(request).also { cacheMutex.withLock { recordsCache.clear() } }

    override suspend fun deleteRecord(id: Int) {
        api.deleteRecord(id)
        cacheMutex.withLock { recordsCache.clear() }
    }

    override suspend fun updateRecord(request: UpdateRecordCommand) {
        api.updateRecord(request)
        cacheMutex.withLock { recordsCache.clear() }
    }

    override suspend fun comparison(from: String?, to: String?): Comparison = onlineFirst(
        remote = { api.comparison(from, to).also { result -> cacheMutex.withLock {
            val key = from to to
            comparisonCache.remove(key)
            comparisonCache[key] = result
            while (comparisonCache.size > 3) comparisonCache.remove(comparisonCache.keys.first())
        } } },
        cached = {
            cacheMutex.withLock { comparisonCache[from to to] }
                ?: throw IllegalStateException("No hay una comparativa guardada para mostrar sin conexión.")
        },
    )

    private suspend fun clearStudentsAndRecords() = cacheMutex.withLock {
        studentCache = emptyList()
        recordsCache.clear()
    }

    private suspend fun <T> onlineFirst(
        remote: suspend () -> T,
        cached: suspend () -> T,
    ): T = runCatching { remote() }.getOrElse { failure ->
        // A denied/expired session or invalid filter is not an offline response.
        // Never turn 401/403 into apparently successful cached data.
        if (failure is CancellationException || failure is ClientRequestException) throw failure
        val value = cached()
        if (hasUsableCache(value)) value else throw failure
    }

    private fun hasUsableCache(value: Any?): Boolean = when (value) {
        null -> false
        is Collection<*> -> value.isNotEmpty()
        else -> true
    }
}
