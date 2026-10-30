package pe.edu.hzg.tesoreria.platform

/** Native Unicode normalization and localized currency formatting. */
expect fun normalizedText(value: String): String
expect fun currencyText(value: Double, currency: String = "PEN"): String
