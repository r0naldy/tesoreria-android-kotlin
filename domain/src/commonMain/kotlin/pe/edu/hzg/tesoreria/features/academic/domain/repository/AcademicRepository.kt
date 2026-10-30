package pe.edu.hzg.tesoreria.features.academic.domain.repository

import pe.edu.hzg.tesoreria.features.academic.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface AcademicRepository {

    suspend fun students(search: String? = null): List<Student>
    suspend fun lookupDni(dni: String): DniLookup
    suspend fun createStudent(request: CreateStudentCommand): Int
    suspend fun updateStudent(request: UpdateStudentCommand): Unit
    suspend fun deleteStudent(id: Int): Unit
    suspend fun withdrawalReasons(): List<String>
    suspend fun ratifyStudent(id: Int): Ratification
    suspend fun withdrawStudent(id: Int, request: WithdrawalCommand): WithdrawalResult
    suspend fun programs(): List<AcademicProgram>
    suspend fun shifts(): List<Shift>
    suspend fun periods(): List<Period>
    suspend fun activePeriod(): Period?
    suspend fun periodSummary(id: Int): PeriodSummary
    suspend fun periodDetail(id: Int): PeriodDetail
    suspend fun openPeriod(request: OpenPeriodCommand): Int
    suspend fun closePeriod(id: Int): PeriodSummary
    suspend fun records(periodId: Int? = null): List<Record>
    suspend fun roster(id: Int, semester: String? = null): EnrollmentRoster
    suspend fun recordExcel(id: Int, semester: String? = null): ByteArray
    suspend fun createRecord(request: CreateRecordCommand): Int
    suspend fun deleteRecord(id: Int): Unit
    suspend fun updateRecord(request: UpdateRecordCommand): Unit
    suspend fun comparison(from: String?, to: String?): Comparison
}
