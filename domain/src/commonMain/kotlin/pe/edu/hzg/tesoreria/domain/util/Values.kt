package pe.edu.hzg.tesoreria.domain.util

import kotlinx.datetime.*

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
fun LocalDate.Companion.now(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.withDayOfMonth(day: Int): LocalDate = LocalDate(year, monthNumber, day)
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other

/** Locale-independent amounts used in CSV, forms and financial documents. */
fun decimal(value: Double, places: Int = 2): String {
    require(value.isFinite()) { "El importe debe ser finito." }
    require(places in 0..6)
    // Round decimal digits, avoiding binary multiplication errors such as 1.005 * 100.
    val raw = value.toString()
    val sign = if (raw.startsWith('-')) "-" else ""
    val parts = raw.removePrefix("-").lowercase().split('e')
    val mantissa = parts[0]
    val exponent = parts.getOrNull(1)?.toInt() ?: 0
    val digits = mantissa.replace(".", "")
    val point = mantissa.indexOf('.').let { if (it < 0) mantissa.length else it } + exponent
    val plain = when {
        point <= 0 -> "0." + "0".repeat(-point) + digits
        point >= digits.length -> digits + "0".repeat(point - digits.length) + ".0"
        else -> digits.substring(0, point) + "." + digits.substring(point)
    }
    val whole = plain.substringBefore('.').trimStart('0').ifEmpty { "0" }
    val fraction = plain.substringAfter('.').padEnd(places + 1, '0')
    val rounded = (whole + fraction.take(places)).toMutableList()
    if (fraction[places] >= '5') {
        var index = rounded.lastIndex
        while (index >= 0 && rounded[index] == '9') { rounded[index] = '0'; index-- }
        if (index < 0) rounded.add(0, '1') else rounded[index] = rounded[index] + 1
    }
    val result = rounded.joinToString("")
    return sign + if (places == 0) result else result.dropLast(places) + "." + result.takeLast(places)
}
