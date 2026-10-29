package pe.edu.hzg.tesoreria.ui.screens

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale

internal enum class CatalogStateFilter(val label: String) {
    ALL("Todos"), ACTIVE("Activos"), INACTIVE("Inactivos");
    fun includes(active: Boolean): Boolean = when (this) {
        ALL -> true
        ACTIVE -> active
        INACTIVE -> !active
    }
}

private val combiningMarks = Regex("\\p{M}+")
private fun comparableCatalogText(value: String): String =
    combiningMarks.replace(Normalizer.normalize(value.trim(), Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

internal fun matchesCatalogQuery(name: String, query: String): Boolean =
    comparableCatalogText(name).contains(comparableCatalogText(query))

/** The API stores prices as numeric(10,2); never silently round user input or accept NaN. */
internal fun parseCatalogPrice(raw: String): Double? {
    val value = raw.trim().replace(',', '.')
    if (!Regex("[0-9]{1,8}(?:\\.[0-9]{1,2})?").matches(value)) return null
    return value.toBigDecimalOrNull()?.takeIf { it <= BigDecimal("99999999.99") }?.toDouble()
}

internal fun formatCatalogPriceInput(price: Double): String = String.format(Locale.US, "%.2f", price)
internal fun formatCatalogPrice(price: Double): String = "S/ ${formatCatalogPriceInput(price)}"
