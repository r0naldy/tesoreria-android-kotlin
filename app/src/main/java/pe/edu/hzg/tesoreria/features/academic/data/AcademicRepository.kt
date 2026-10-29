package pe.edu.hzg.tesoreria.features.academic.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import io.ktor.client.plugins.ClientRequestException

/**
 * Repositorio online-first con respaldo en memoria para lecturas ya vistas.
 * Las mutaciones críticas son llamadas directas: no se encolan, no se repiten
 * automáticamente y el servidor siempre decide si se aplicaron.
 */
class AcademicRepository(private val api: AcademicApi) {
    private val cacheMutex = Mutex()
    private var studentCache: List<StudentDto> = emptyList()
    private var periodsCache: List<PeriodDto> = emptyList()
    private val recordsCache = mutableMapOf<Int?, List<RecordDto>>()
    private var programsCache: List<AcademicProgramDto> = emptyList()
    private var shiftsCache: List<ShiftDto> = emptyList()
    private var reasonsCache: List<String> = emptyList()
    private val comparisonCache = mutableMapOf<Pair<String?, String?>, ComparisonDto>()

    suspend fun students(search: String? = null): List<StudentDto> = onlineFirst(
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

    suspend fun lookupDni(dni: String) = api.lookupDni(dni)

    suspend fun createStudent(request: CreateStudentRequest): Int =
        api.createStudent(request).also { clearStudentsAndRecords() }

    suspend fun updateStudent(request: UpdateStudentRequest) {
        api.updateStudent(request)
        clearStudentsAndRecords()
    }

    suspend fun deleteStudent(id: Int) {
        api.deleteStudent(id)
        clearStudentsAndRecords()
    }

    suspend fun withdrawalReasons(): List<String> = onlineFirst(
        remote = { api.withdrawalReasons().also { cacheMutex.withLock { reasonsCache = it } } },
        cached = { cacheMutex.withLock { reasonsCache } },
    )

    suspend fun ratifyStudent(id: Int): RatificationDto =
        api.ratifyStudent(id).also { clearStudentsAndRecords() }

    suspend fun withdrawStudent(id: Int, request: WithdrawalRequest): WithdrawalResultDto =
        api.withdrawStudent(id, request).also { clearStudentsAndRecords() }

    suspend fun programs(): List<AcademicProgramDto> = onlineFirst(
        remote = { api.programs().also { cacheMutex.withLock { programsCache = it } } },
        cached = { cacheMutex.withLock { programsCache } },
    )

    suspend fun shifts(): List<ShiftDto> = onlineFirst(
        remote = { api.shifts().also { cacheMutex.withLock { shiftsCache = it } } },
        cached = { cacheMutex.withLock { shiftsCache } },
    )

    suspend fun periods(): List<PeriodDto> = onlineFirst(
        remote = { api.periods().also { cacheMutex.withLock { periodsCache = it } } },
        cached = { cacheMutex.withLock { periodsCache } },
    )

    suspend fun activePeriod(): PeriodDto? = runCatching { api.activePeriod() }
        .getOrElse { error ->
            if (error is CancellationException || error is ClientRequestException) throw error
            cacheMutex.withLock {
                periodsCache.firstOrNull { it.estado.equals("Abierto", ignoreCase = true) }
            } ?: throw error
        }

    suspend fun periodSummary(id: Int) = api.periodSummary(id)
    suspend fun periodDetail(id: Int) = api.periodDetail(id)

    suspend fun openPeriod(request: OpenPeriodRequest): Int =
        api.openPeriod(request).also { cacheMutex.withLock { periodsCache = emptyList() } }

    suspend fun closePeriod(id: Int): PeriodSummaryDto =
        api.closePeriod(id).also { cacheMutex.withLock { periodsCache = emptyList() } }

    suspend fun records(periodId: Int? = null): List<RecordDto> = onlineFirst(
        remote = { api.records(periodId).also { cacheMutex.withLock { recordsCache[periodId] = it } } },
        cached = { cacheMutex.withLock { recordsCache[periodId].orEmpty() } },
    )

    suspend fun roster(id: Int, semester: String? = null) = api.roster(id, semester)
    suspend fun recordExcel(id: Int, semester: String? = null) = api.recordExcel(id, semester)

    suspend fun createRecord(request: CreateRecordRequest): Int =
        api.createRecord(request).also { cacheMutex.withLock { recordsCache.clear() } }

    suspend fun deleteRecord(id: Int) {
        api.deleteRecord(id)
        cacheMutex.withLock { recordsCache.clear() }
    }

    suspend fun updateRecord(request: UpdateRecordRequest) {
        api.updateRecord(request)
        cacheMutex.withLock { recordsCache.clear() }
    }

    suspend fun comparison(from: String?, to: String?): ComparisonDto = onlineFirst(
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
