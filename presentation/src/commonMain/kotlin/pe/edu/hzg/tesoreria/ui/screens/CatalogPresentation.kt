package pe.edu.hzg.tesoreria.ui.screens

import pe.edu.hzg.tesoreria.platform.normalizedText
import pe.edu.hzg.tesoreria.domain.util.decimal

internal enum class CatalogStateFilter(val label: String) {
    ALL("Todos"), ACTIVE("Activos"), INACTIVE("Inactivos");
    fun includes(active: Boolean): Boolean = when (this) {
        ALL -> true
        ACTIVE -> active
        INACTIVE -> !active
    }
}

private fun comparableCatalogText(value: String): String = normalizedText(value.trim()).lowercase()

internal fun matchesCatalogQuery(name: String, query: String): Boolean =
    comparableCatalogText(name).contains(comparableCatalogText(query))

/** The API stores prices as numeric(10,2); never silently round user input or accept NaN. */
internal fun parseCatalogPrice(raw: String): Double? {
    val value = raw.trim().replace(',', '.')
    if (!Regex("[0-9]{1,8}(?:\\.[0-9]{1,2})?").matches(value)) return null
    return value.toDoubleOrNull()?.takeIf { it.isFinite() && it <= 99999999.99 }
}

internal fun formatCatalogPriceInput(price: Double): String = decimal(price)
internal fun formatCatalogPrice(price: Double): String = "S/ ${formatCatalogPriceInput(price)}"
