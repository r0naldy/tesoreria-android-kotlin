package pe.edu.hzg.tesoreria.features.treasury.domain.repository

import pe.edu.hzg.tesoreria.features.treasury.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface TreasuryRepository {
    val dashboard: StateFlow<TreasuryDashboard?>
    val programs: StateFlow<List<TreasuryProgram>>
    val services: StateFlow<List<TreasuryService>>
    val shifts: StateFlow<List<Shift>>
    val period: StateFlow<ActivePeriod?>
    val registers: StateFlow<List<EnrollmentRegister>>
    suspend fun refreshDashboard(): TreasuryDashboard
    suspend fun dashboardPeriod(): DashboardPeriod
    suspend fun bootstrapCashier(): Unit
    suspend fun lookupStudent(dni: String): StudentLookup
    fun cachedStudent(dni: String): StudentLookup?
    suspend fun createStudent(request: CreateStudentCommand): StudentLookup
    suspend fun emitTicket(studentId: Int, lines: List<EmitTicketDetailCommand>): Ticket
    suspend fun tickets(from: String?, to: String?, dni: String?): List<TicketListItem>
    suspend fun ticket(id: Int): Ticket
    suspend fun configuration(): InstitutionConfig
    suspend fun incomeReport(from: String?, to: String?): IncomeReport
    suspend fun expenses(from: String?, to: String?, category: String?): List<ExpenseListItem>
    suspend fun expense(id: Int): Expense
}
