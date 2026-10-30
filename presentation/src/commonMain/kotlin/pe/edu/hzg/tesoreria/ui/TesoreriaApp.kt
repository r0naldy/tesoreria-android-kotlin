package pe.edu.hzg.tesoreria.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.hzg.tesoreria.admin.ui.AuditoriaScreen
import pe.edu.hzg.tesoreria.admin.ui.ConfiguracionScreen
import pe.edu.hzg.tesoreria.admin.ui.UsuariosScreen
import pe.edu.hzg.tesoreria.domain.model.AuthSession
import pe.edu.hzg.tesoreria.features.academic.ui.ComparisonScreen
import pe.edu.hzg.tesoreria.features.academic.ui.PeriodsScreen
import pe.edu.hzg.tesoreria.features.academic.ui.RecordsScreen
import pe.edu.hzg.tesoreria.features.academic.ui.StudentsScreen
import pe.edu.hzg.tesoreria.features.treasury.ui.CashierScreen
import pe.edu.hzg.tesoreria.features.treasury.ui.IncomeReportScreen
import pe.edu.hzg.tesoreria.features.treasury.ui.TicketHistoryScreen
import pe.edu.hzg.tesoreria.presentation.AppViewModel
import pe.edu.hzg.tesoreria.presentation.SsoLoginUiState
import pe.edu.hzg.tesoreria.security.SsoDeepLinkResult
import pe.edu.hzg.tesoreria.ui.screens.DashboardScreen
import pe.edu.hzg.tesoreria.ui.screens.EgresosScreen
import pe.edu.hzg.tesoreria.ui.screens.LoginScreen
import pe.edu.hzg.tesoreria.ui.screens.ProgramasScreen
import pe.edu.hzg.tesoreria.ui.screens.ServiciosScreen
import pe.edu.hzg.tesoreria.ui.screens.SsoAccessScreen
import pe.edu.hzg.tesoreria.ui.theme.InstitutionalNavyDeep

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val group: String,
    val adminOnly: Boolean = false,
)

private val allDestinations = listOf(
    Destination("tesoreria", "Tesorería", Icons.Rounded.PointOfSale, "Caja"),
    Destination("dashboard", "Dashboard", Icons.Rounded.Dashboard, "Caja"),
    Destination("tickets", "Tickets", Icons.Rounded.ConfirmationNumber, "Caja"),
    Destination("reportes", "Reporte de ingresos", Icons.Rounded.QueryStats, "Caja"),
    Destination("egresos", "Egresos", Icons.Rounded.AccountBalanceWallet, "Caja"),
    Destination("comparativa", "Comparativa", Icons.AutoMirrored.Rounded.CompareArrows, "Caja"),
    Destination("programas", "Programas", Icons.Rounded.School, "Catálogos"),
    Destination("servicios", "Servicios", Icons.Rounded.Payments, "Catálogos"),
    Destination("alumnos", "Estudiantes", Icons.Rounded.Groups, "Académico"),
    Destination("registros", "Registros", Icons.AutoMirrored.Rounded.Assignment, "Académico"),
    Destination("periodos", "Períodos", Icons.Rounded.CalendarMonth, "Académico"),
    Destination("usuarios", "Usuarios", Icons.Rounded.ManageAccounts, "Administración", adminOnly = true),
    Destination("configuracion", "Configuración", Icons.Rounded.Settings, "Administración"),
    Destination("auditoria", "Auditoría", Icons.Rounded.Shield, "Administración", adminOnly = true),
)

private val bottomRoutes = listOf("tesoreria", "dashboard", "tickets", "egresos")

@Composable
fun TesoreriaApp(
    pendingSsoLink: SsoDeepLinkResult? = null,
    onSsoLinkConsumed: () -> Unit = {},
    appViewModel: AppViewModel = koinViewModel(),
) {
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val ssoState by appViewModel.ssoLoginState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingSsoLink) {
        when (val link = pendingSsoLink) {
            is SsoDeepLinkResult.Valid -> appViewModel.loginFromAsisto(link.token)
            is SsoDeepLinkResult.Invalid -> appViewModel.showInvalidSsoLink(link.userMessage)
            SsoDeepLinkResult.NotSsoLink, null -> Unit
        }
        if (pendingSsoLink != null) onSsoLinkConsumed()
    }

    when (val currentSsoState = ssoState) {
        SsoLoginUiState.Idle -> {
            if (session == null) LoginScreen()
            else AuthenticatedApp(requireNotNull(session), appViewModel::logout)
        }
        else -> SsoAccessScreen(currentSsoState, appViewModel::dismissSsoMessage)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedApp(session: AuthSession, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var accountMenu by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "tesoreria"
    val selected = allDestinations.firstOrNull { it.route == currentRoute } ?: allDestinations.first()
    val isAdmin = session.rol.equals("Administrador", ignoreCase = true)
    val visibleDestinations = allDestinations.filter { !it.adminOnly || isAdmin }

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 340.dp),
                drawerContainerColor = InstitutionalNavyDeep,
                drawerContentColor = Color(0xFFEEF2FB),
            ) {
                Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                        Text("TESORERÍA", style = MaterialTheme.typography.labelLarge, color = Color(0xFF80D5D1))
                        Text("Gestión institucional", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB5C2D5))
                        Spacer(Modifier.height(20.dp))
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    session.nombreCompleto.ifBlank { session.username }.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            session.nombreCompleto.ifBlank { session.username },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            session.rol,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB5C2D5),
                        )
                    }
                    visibleDestinations.groupBy { it.group }.forEach { (group, entries) ->
                        Text(
                            group,
                            modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF80D5D1),
                        )
                        entries.forEach { destination ->
                            NavigationDrawerItem(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navigate(destination.route)
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFF265F63),
                                    selectedTextColor = Color.White,
                                    selectedIconColor = Color.White,
                                    unselectedContainerColor = Color.Transparent,
                                    unselectedTextColor = Color(0xFFD4DEEF),
                                    unselectedIconColor = Color(0xFF97A5C2),
                                ),
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        selected = false,
                        onClick = { confirmLogout = true },
                        icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                        label = { Text("Cerrar sesión") },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color(0xFFD4DEEF),
                            unselectedIconColor = Color(0xFF97A5C2),
                        ),
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    title = {
                        Column {
                            Text(selected.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${session.nombreCompleto.ifBlank { session.username }} · ${session.rol}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { accountMenu = true }) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                        Text(session.nombreCompleto.ifBlank { session.username }.take(1).uppercase(), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Cerrar sesión") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                                    onClick = { accountMenu = false; confirmLogout = true },
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    bottomRoutes.mapNotNull { route -> allDestinations.firstOrNull { it.route == route } }
                        .forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigate(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) },
                            )
                        }
                    NavigationBarItem(
                        selected = currentRoute !in bottomRoutes,
                        onClick = { scope.launch { drawerState.open() } },
                        icon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null) },
                        label = { Text("Más") },
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                NavHost(
                    navController = navController,
                    startDestination = "tesoreria",
                    modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth().fillMaxHeight(),
                ) {
                    composable("tesoreria") { CashierScreen() }
                    composable("dashboard") { DashboardScreen(session) }
                    composable("tickets") { TicketHistoryScreen() }
                    composable("reportes") { IncomeReportScreen() }
                    composable("egresos") { EgresosScreen() }
                    composable("comparativa") { ComparisonScreen() }
                    composable("programas") { ProgramasScreen(canEdit = isAdmin) }
                    composable("servicios") { ServiciosScreen(canEdit = isAdmin) }
                    composable("alumnos") { StudentsScreen(isAdmin = isAdmin) }
                    composable("registros") { RecordsScreen(isAdmin = isAdmin) }
                    composable("periodos") { PeriodsScreen(isAdmin = isAdmin) }
                    composable("usuarios") { UsuariosScreen(isAdmin = isAdmin, currentUsername = session.username) }
                    composable("configuracion") { ConfiguracionScreen(canEdit = isAdmin) }
                    composable("auditoria") { AuditoriaScreen(isAdmin = isAdmin) }
                }
            }
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Los comprobantes pendientes se conservan de forma privada y solo se sincronizarán al volver a ingresar con esta cuenta.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Continuar aquí") } },
        )
    }
}
