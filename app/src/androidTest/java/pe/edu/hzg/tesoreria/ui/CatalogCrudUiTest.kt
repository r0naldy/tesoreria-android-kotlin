package pe.edu.hzg.tesoreria.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pe.edu.hzg.tesoreria.domain.model.Programa
import pe.edu.hzg.tesoreria.domain.model.Servicio
import pe.edu.hzg.tesoreria.domain.repository.ProgramaRepository
import pe.edu.hzg.tesoreria.domain.repository.ServicioRepository
import pe.edu.hzg.tesoreria.presentation.ProgramasViewModel
import pe.edu.hzg.tesoreria.presentation.ServiciosViewModel
import pe.edu.hzg.tesoreria.ui.screens.ProgramasScreen
import pe.edu.hzg.tesoreria.ui.screens.ServiciosScreen
import pe.edu.hzg.tesoreria.ui.theme.TesoreriaTheme

/** Real Compose/Android interactions with in-memory repositories: never writes to production. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class CatalogCrudUiTest {
    @get:Rule(order = 0) val compose = createComposeRule()
    @get:Rule(order = 1) val failureScreenshot = object : TestWatcher() {
        override fun failed(error: Throwable?, description: Description) {
            runCatching {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
                val folder = File(instrumentation.targetContext.cacheDir, "qa-catalogos").apply { mkdirs() }
                File(folder, "failed-${description.methodName}.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Test fun programs_create_edit_deactivate_delete() {
        val repository = FakePrograms()
        val viewModel = ProgramasViewModel(repository)
        compose.setContent { TesoreriaTheme { ProgramasScreen(canEdit = true, viewModel) } }
        compose.onNodeWithText("Nuevo programa").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre *")).performScrollTo().performTextInput("Programa de prueba local")
        screenshot("programa-crear")
        tapStableButton("Crear")
        compose.waitUntil { repository.values.value.size == 1 && viewModel.state.value.mutationRevision == 1 }
        compose.onNodeWithText("Crear").assertDoesNotExist()
        showCatalogItem(101)
        compose.onNodeWithText("Programa de prueba local").assertExists()
        compose.onNode(hasContentDescription("Editar Programa de prueba local")).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre *")).performScrollTo().performTextReplacement("Programa editado")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        tapStableButton("Guardar cambios")
        compose.waitUntil { repository.values.value.singleOrNull()?.nombre == "Programa editado" && viewModel.state.value.mutationRevision == 2 }
        compose.onNodeWithText("Guardar cambios").assertDoesNotExist()
        assertEquals("Inactivo", repository.values.value.single().estado)
        showCatalogItem(101)
        screenshot("programas-listado")
        compose.onNode(hasContentDescription("Eliminar Programa editado")).performScrollTo().performClick()
        compose.onNodeWithText("Sí, eliminar").performClick()
        compose.waitUntil { repository.values.value.isEmpty() }
        compose.onNodeWithText("Programa editado").assertDoesNotExist()
        assertEquals(listOf("create", "update", "delete"), repository.operations)
    }

    @Test fun rejected_creation_keeps_form_values_and_server_error() {
        val repository = FakePrograms().apply { failCreate = true }
        val viewModel = ProgramasViewModel(repository)
        compose.setContent { TesoreriaTheme { ProgramasScreen(canEdit = true, viewModel) } }
        compose.onNodeWithText("Nuevo programa").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre *")).performScrollTo().performTextInput("Nombre que debe conservarse")
        tapStableButton("Crear")
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.mutationError == "Ya existe un programa con ese nombre." && !viewModel.state.value.isSaving
        }
        assertEquals(listOf("create"), repository.operations)
        assertEquals(0, viewModel.state.value.mutationRevision)
        compose.onNodeWithText("Ya existe un programa con ese nombre.").assertExists()
        compose.onNode(hasSetTextAction() and hasText("Nombre *")).assertTextContains("Nombre que debe conservarse")
        compose.onNodeWithText("Crear").assertExists()
        assertTrue(repository.values.value.isEmpty())
        screenshot("programa-error-conserva-datos")
    }

    @Test fun services_create_edit_delete_and_validate_price() {
        val repository = FakeServices()
        val viewModel = ServiciosViewModel(repository)
        compose.setContent { TesoreriaTheme { ServiciosScreen(canEdit = true, viewModel) } }
        compose.onNodeWithText("Nuevo servicio").performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre *")).performScrollTo().performTextInput("Servicio de prueba local")
        compose.onNode(hasSetTextAction() and hasText("Precio *")).performScrollTo().performTextInput("12.345")
        compose.onNodeWithText("Crear").assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("Precio *")).performScrollTo().performTextReplacement("12,50")
        tapStableButton("Crear")
        compose.waitUntil { repository.values.value.size == 1 && viewModel.state.value.mutationRevision == 1 }
        compose.onNodeWithText("Crear").assertDoesNotExist()
        assertEquals(12.5, repository.values.value.single().precio, 0.0)
        showCatalogItem(102)
        compose.onNode(hasContentDescription("Editar Servicio de prueba local")).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Nombre *")).performScrollTo().performTextReplacement("Servicio actualizado")
        compose.onNode(hasSetTextAction() and hasText("Precio *")).performScrollTo().performTextReplacement("0")
        screenshot("servicio-editar")
        tapStableButton("Guardar cambios")
        compose.waitUntil { repository.values.value.singleOrNull()?.nombre == "Servicio actualizado" && viewModel.state.value.mutationRevision == 2 }
        compose.onNodeWithText("Guardar cambios").assertDoesNotExist()
        assertEquals(0.0, repository.values.value.single().precio, 0.0)
        showCatalogItem(102)
        screenshot("servicios-listado")
        compose.onNode(hasContentDescription("Eliminar Servicio actualizado")).performScrollTo().performClick()
        compose.onNodeWithText("Sí, eliminar").performClick()
        compose.waitUntil { repository.values.value.isEmpty() }
        assertEquals(listOf("create", "update", "delete"), repository.operations)
    }

    @Test fun rejected_deletion_keeps_item_and_confirmation_visible() {
        val repository = FakeServices().apply {
            values.value = listOf(Servicio(21, "Servicio con tickets", 15.0, "Activo"))
            failDelete = true
        }
        val viewModel = ServiciosViewModel(repository)
        compose.setContent { TesoreriaTheme { ServiciosScreen(canEdit = true, viewModel) } }
        showCatalogItem(21)
        compose.onNode(hasContentDescription("Eliminar Servicio con tickets")).performScrollTo().performClick()
        compose.onNodeWithText("Sí, eliminar").performClick()
        compose.onNodeWithText("El servicio tiene tickets asociados; desactívelo.").assertExists()
        compose.onNodeWithText("Sí, eliminar").assertExists()
        assertEquals(1, repository.values.value.size)
        screenshot("servicio-eliminar-protegido")
    }

    @Test fun finance_role_cannot_mutate_catalogs() {
        val repository = FakePrograms().apply {
            values.value = listOf(Programa(31, "Programa de consulta", "Activo", "2026-01-01"))
        }
        val viewModel = ProgramasViewModel(repository)
        compose.setContent { TesoreriaTheme { ProgramasScreen(canEdit = false, viewModel) } }
        showCatalogItem("header")
        compose.onNodeWithText("Nuevo programa").assertDoesNotExist()
        showCatalogItem(31)
        compose.onNodeWithText("Programa de consulta").assertExists()
        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Eliminar").assertDoesNotExist()
        showCatalogItem("filters")
        compose.onNodeWithText("Solo consulta · los cambios requieren un administrador").assertExists()
        screenshot("catalogo-finanzas-solo-consulta")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // Capture the Android window stack, including dialogs and the keyboard.
        // Capturing a Compose dialog root alone may crop the underlying activity.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val context = instrumentation.targetContext
        val folder = File(context.cacheDir, "qa-catalogos").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Lazy grids intentionally do not compose rows below the viewport. Scroll by the stable
     * dataset key first, then locate and touch that row's visible controls. */
    private fun showCatalogItem(key: Any) {
        compose.onNodeWithTag("catalog-grid").performScrollToKey(key)
        compose.waitForIdle()
    }

    /** Compose idleness does not include Android's IME/inset animation. Wait for the real target
     * bounds to settle before injecting one touch, without dismissing or replacing the keyboard. */
    private fun tapStableButton(label: String) {
        val target = compose.onNodeWithText(label)
        target.assertIsDisplayed().assertIsEnabled()
        var previousBounds: Rect? = null
        val startedAt = SystemClock.elapsedRealtime()
        var stableSince = startedAt
        compose.waitUntil(timeoutMillis = 5_000) {
            val currentBounds = target.fetchSemanticsNode().boundsInRoot
            val now = SystemClock.elapsedRealtime()
            if (previousBounds != currentBounds) {
                previousBounds = currentBounds
                stableSince = now
            }
            now - startedAt >= 700 && now - stableSince >= 450
        }
        target.assertIsDisplayed().assertIsEnabled().performTouchInput { click() }
    }

    private class FakePrograms : ProgramaRepository {
        override suspend fun discardPending(id: Int): Result<Unit> = Result.success(Unit)
        val values = MutableStateFlow<List<Programa>>(emptyList())
        val operations = mutableListOf<String>()
        var failCreate = false
        override fun observeProgramas(soloActivos: Boolean): Flow<List<Programa>> = values.map { items -> items.filter { !soloActivos || it.activo } }
        override suspend fun refresh(soloActivos: Boolean) = Result.success(Unit)
        override suspend fun create(nombre: String): Result<Int> {
            operations += "create"
            if (failCreate) return Result.failure(IllegalArgumentException("Ya existe un programa con ese nombre."))
            values.value = values.value + Programa(101, nombre, "Activo", "2026-01-01")
            return Result.success(101)
        }
        override suspend fun update(programa: Programa): Result<Unit> {
            operations += "update"
            values.value = values.value.map { if (it.id == programa.id) programa else it }
            return Result.success(Unit)
        }
        override suspend fun delete(id: Int): Result<Unit> {
            operations += "delete"
            values.value = values.value.filterNot { it.id == id }
            return Result.success(Unit)
        }
    }

    private class FakeServices : ServicioRepository {
        override suspend fun discardPending(id: Int): Result<Unit> = Result.success(Unit)
        val values = MutableStateFlow<List<Servicio>>(emptyList())
        val operations = mutableListOf<String>()
        var failDelete = false
        override fun observeServicios(soloActivos: Boolean): Flow<List<Servicio>> = values.map { items -> items.filter { !soloActivos || it.activo } }
        override suspend fun refresh(soloActivos: Boolean) = Result.success(Unit)
        override suspend fun create(nombre: String, precio: Double): Result<Int> {
            operations += "create"
            values.value = values.value + Servicio(102, nombre, precio, "Activo")
            return Result.success(102)
        }
        override suspend fun update(servicio: Servicio): Result<Unit> {
            operations += "update"
            values.value = values.value.map { if (it.id == servicio.id) servicio else it }
            return Result.success(Unit)
        }
        override suspend fun delete(id: Int): Result<Unit> {
            operations += "delete"
            if (failDelete) return Result.failure(IllegalStateException("El servicio tiene tickets asociados; desactívelo."))
            values.value = values.value.filterNot { it.id == id }
            return Result.success(Unit)
        }
    }
}
