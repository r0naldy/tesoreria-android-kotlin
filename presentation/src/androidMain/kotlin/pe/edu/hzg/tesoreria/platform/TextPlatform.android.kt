package pe.edu.hzg.tesoreria.platform

import java.text.Normalizer
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

actual fun normalizedText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
actual fun currencyText(value: Double, currency: String): String =
    NumberFormat.getCurrencyInstance(Locale("es", "PE")).apply {
        this.currency = Currency.getInstance(if (currency == "USD") "USD" else "PEN")
    }.format(value)
