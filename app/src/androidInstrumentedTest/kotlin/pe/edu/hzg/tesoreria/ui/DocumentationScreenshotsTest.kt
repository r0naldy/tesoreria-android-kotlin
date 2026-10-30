package pe.edu.hzg.tesoreria.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import pe.edu.hzg.tesoreria.domain.model.*
import pe.edu.hzg.tesoreria.domain.repository.*
import pe.edu.hzg.tesoreria.presentation.*
import pe.edu.hzg.tesoreria.features.treasury.domain.model.*
import pe.edu.hzg.tesoreria.features.treasury.domain.repository.TreasuryRepository
import pe.edu.hzg.tesoreria.features.treasury.presentation.*
import pe.edu.hzg.tesoreria.features.treasury.ui.*
import pe.edu.hzg.tesoreria.ui.screens.*
import pe.edu.hzg.tesoreria.ui.theme.TesoreriaTheme

/** Documentation captures of production composables, with local fictional data.
 * No network, real credentials, production writes, or changes to FLAG_SECURE.
 */
@RunWith(AndroidJUnit4::class)
class DocumentationScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val errors = object : ErrorMessages {
        override suspend fun academic(error: Throwable) = error.message.orEmpty()
        override fun treasury(error: Throwable) = error.message.orEmpty()
    }
    private fun show(content: @Composable () -> Unit) {
        compose.setContent { TesoreriaTheme { Surface { content() } } }
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "documentation").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        check(bitmap.width > 500 && bitmap.height > 1000)
    }
    @Test fun login() {
        val auth = object : AuthRepository {
            override fun observeSession(): Flow<AuthSession?> = MutableStateFlow(null)
            override suspend fun login(username: String, password: String): Result<AuthSession> = Result.failure(IllegalArgumentException("Cuenta de demostración sin acceso al servidor."))
            override suspend fun loginFromAsisto(token: String): Result<AuthSession> = Result.failure(IllegalStateException("No disponible en esta captura"))
            override suspend fun logout() = Unit
        }
        val identity = object : InstitutionalRepository {
            override suspend fun getIdentity() = Result.success(InstitutionalIdentity("CETPRO Horacio Zeballos Gámez", "Juliaca", null))
        }
        val vm = LoginViewModel(auth, identity)
        show { LoginScreen(vm) }
        capture("01-acceso")
        compose.runOnIdle { vm.login() }
        compose.onNodeWithText("Ingrese usuario y contraseña.").assertExists()
        capture("02-validacion-acceso")
    }
    @Test fun cashier() {
        val vm = CashierViewModel(DemoTreasury(), errors)
        show { CashierScreen(vm) }
        compose.waitUntil { !vm.state.value.loadingCatalogs }
        capture("03-caja")
    }
    @Test fun dashboard() {
        val vm = TreasuryDashboardViewModel(DemoTreasury(), errors)
        show { DashboardScreen(AuthSession("demo-no-valido", "demo", "Operador de demostración", "Administrador", Long.MAX_VALUE), vm) }
        compose.waitUntil { !vm.state.value.loading }
        capture("04-dashboard")
    }
    @Test fun tickets() {
        val vm = TicketHistoryViewModel(DemoTreasury(), errors)
        show { TicketHistoryScreen(vm) }
        compose.waitUntil { vm.state.value.items.isNotEmpty() }
        capture("05-tickets")
    }
    @Test fun income() {
        val vm = IncomeReportViewModel(DemoTreasury(), errors)
        show { IncomeReportScreen(vm) }
        compose.runOnIdle { vm.load("2026-10-01", "2026-10-29") }
        compose.waitUntil { vm.state.value.report != null }
        capture("06-ingresos")
    }
    @Test fun programs() {
        val repo = object : ProgramaRepository {
            override fun observeProgramas(soloActivos: Boolean): Flow<List<Programa>> = MutableStateFlow(listOf(Programa(1,"Computación e Informática","Activo","2026-01-01"),Programa(2,"Electricidad","Activo","2026-01-01")))
            override suspend fun refresh(soloActivos: Boolean) = Result.success(Unit)
            override suspend fun create(nombre: String) = Result.success(3)
            override suspend fun update(programa: Programa) = Result.success(Unit)
            override suspend fun delete(id: Int) = Result.success(Unit)
            override suspend fun discardPending(id: Int) = Result.success(Unit)
        }
        val vm = ProgramasViewModel(repo)
        show { ProgramasScreen(true, vm) }
        capture("07-programas")
    }
    @Test fun services() {
        val repo = object : ServicioRepository {
            override fun observeServicios(soloActivos: Boolean): Flow<List<Servicio>> = MutableStateFlow(listOf(Servicio(1,"Matrícula de demostración",50.0,"Activo"),Servicio(2,"Constancia de estudios",15.0,"Activo")))
            override suspend fun refresh(soloActivos: Boolean) = Result.success(Unit)
            override suspend fun create(nombre: String, precio: Double) = Result.success(3)
            override suspend fun update(servicio: Servicio) = Result.success(Unit)
            override suspend fun delete(id: Int) = Result.success(Unit)
            override suspend fun discardPending(id: Int) = Result.success(Unit)
        }
        val vm = ServiciosViewModel(repo)
        show { ServiciosScreen(true, vm) }
        capture("08-servicios")
    }
    @Test fun expenses() {
        val repo = object : ComprobanteRepository {
            override fun observeComprobantes(): Flow<List<Comprobante>> = MutableStateFlow(emptyList())
            override suspend fun refresh() = Result.success(Unit)
            override suspend fun findById(id: Int): Result<Comprobante> = Result.failure(IllegalArgumentException("Sin comprobante local"))
            override suspend fun queueForUpload(draft: ComprobanteDraft): Result<String> = Result.failure(IllegalStateException("Captura documental sin envío"))
        }
        val sync = object : SyncRepository {
            override fun observePendingCount(): Flow<Long> = MutableStateFlow(0L)
            override suspend fun synchronize() = Result.success(SyncSummary(0,0,0))
        }
        val vm = EgresosViewModel(repo, sync)
        val browser = ExpenseBrowserViewModel(DemoTreasury(), errors)
        show { EgresosScreen(vm, browser) }
        compose.waitUntil { !browser.state.value.loading }
        capture("09-egresos")
    }
}

private class DemoTreasury : TreasuryRepository {
    private val rows = listOf(TicketListItem(1,"DEMO-0001","0001","2026-10-29T09:30:00","00000000","Estudiante de demostración",50.0,"demo","Emitido"))
    override val dashboard = MutableStateFlow<TreasuryDashboard?>(TreasuryDashboard(totalRecaudadoHoy=50.0,totalRecaudadoMes=50.0,ticketsHoy=1,ticketsMes=1,programasActivos=2,ultimosTickets=rows))
    override val programs = MutableStateFlow(listOf(TreasuryProgram(1,"Computación e Informática")))
    override val services = MutableStateFlow(listOf(TreasuryService(1,"Matrícula de demostración",50.0)))
    override val shifts = MutableStateFlow(listOf(Shift(1,"Mañana")))
    override val period = MutableStateFlow<ActivePeriod?>(ActivePeriod(1,"2026-II","2026-08-01","2026-12-31","Abierto",63))
    override val registers = MutableStateFlow<List<EnrollmentRegister>>(emptyList())
    override suspend fun refreshDashboard() = dashboard.value!!
    override suspend fun dashboardPeriod() = DashboardPeriod(true,"2026-II","2026-08-01","2026-12-31",50.0,1,1,1,63)
    override suspend fun bootstrapCashier() = Unit
    override suspend fun lookupStudent(dni: String) = StudentLookup(dni=dni)
    override fun cachedStudent(dni: String): StudentLookup? = null
    override suspend fun createStudent(request: CreateStudentCommand): StudentLookup = error("Captura sin escrituras")
    override suspend fun emitTicket(studentId: Int, lines: List<EmitTicketDetailCommand>): Ticket = error("Captura sin escrituras")
    override suspend fun tickets(from: String?, to: String?, dni: String?) = rows
    override suspend fun ticket(id: Int): Ticket = error("No utilizado")
    override suspend fun configuration() = InstitutionConfig()
    override suspend fun incomeReport(from: String?, to: String?) = IncomeReport(from,to,1,1,50.0, listOf(IncomeReportItem("2026-10-29","DEMO-0001","0001","00000000","Estudiante de demostración","Computación e Informática","Matrícula de demostración",50.0,"demo")))
    override suspend fun expenses(from: String?, to: String?, category: String?) = listOf(ExpenseListItem(1,proveedor="Proveedor de demostración",numeroComprobante="DEMO-001",fechaRegistro="2026-10-29",total=20.0,categoria="Papeleria",concepto="Material de oficina",estado="Registrado"))
    override suspend fun expense(id: Int): Expense = error("No utilizado")
}
