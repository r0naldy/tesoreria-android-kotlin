package pe.edu.hzg.tesoreria.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import pe.edu.hzg.tesoreria.data.remote.dto.ComprobanteDto
import pe.edu.hzg.tesoreria.db.AppDatabase

class ComprobanteLocalWriterTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `confirmed upload keeps remote image and clears temporary local uri`() {
        val localId = "local-receipt"
        val temporaryUri = "content://pe.edu.hzg.tesoreria.fileprovider/pending_receipt_images/receipt.jpg"
        database.appDatabaseQueries.upsertComprobante(
            local_id = localId,
            remote_id = null,
            proveedor = null,
            ruc = null,
            tipo_documento = null,
            numero_comprobante = null,
            fecha_emision = null,
            hora_emision = null,
            moneda = "PEN",
            subtotal_centimos = null,
            igv_centimos = null,
            total_centimos = 0,
            categoria = null,
            concepto = null,
            metodo_pago = null,
            confianza = 0,
            es_duplicado_probable = 0,
            observaciones = null,
            imagen_uri = temporaryUri,
            imagen_url = null,
            fecha_registro = "2026-10-27T00:00:00Z",
            usuario = "",
            estado = "Pendiente",
            sync_status = "PENDING",
            sync_error = null,
            created_at = 1,
            updated_at = 1,
        )

        ComprobanteLocalWriter(database).upsertDetail(
            dto = ComprobanteDto(
                id = 42,
                total = 15.0,
                imagenUrl = "/api/comprobantes/42/imagen",
                fechaRegistro = "2026-10-27T00:01:00Z",
                estado = "Procesado",
            ),
            preferredLocalId = localId,
            clearLocalImageUri = true,
        )

        val stored = database.appDatabaseQueries
            .selectComprobanteByLocalId(localId)
            .executeAsOne()
        assertEquals(42L, stored.remote_id)
        assertEquals("/api/comprobantes/42/imagen", stored.imagen_url)
        assertEquals("SYNCED", stored.sync_status)
        assertNull(stored.imagen_uri)
    }
}
