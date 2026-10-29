# Integración del bloque Tesorería

Este paquete no modifica la composición raíz para poder integrarse junto con los demás módulos.

1. En `AppModule.kt`, incluir:

   ```kotlin
   includes(treasuryModule(BuildConfig.API_BASE_URL))
   ```

2. En `TesoreriaApp.kt`, agregar destinos/rutas para:

   - `CashierScreen()` — Caja/Tesorería, ruta sugerida `tesoreria`.
   - `TicketHistoryScreen()` — historial, detalle, impresión y compartir; ruta `tickets`.
   - `IncomeReportScreen()` — reporte de ingresos; ruta `reportes`.
   - `DashboardScreen(session)` ya reemplaza la pantalla anterior y consume el dashboard real.
   - `EgresosScreen()` ya conserva captura offline y añade filtros/detalle remoto.

La emisión de tickets es deliberadamente online y no se reintenta: el API actual no acepta una
clave de idempotencia. Las lecturas conservan el último resultado confirmado en memoria.
