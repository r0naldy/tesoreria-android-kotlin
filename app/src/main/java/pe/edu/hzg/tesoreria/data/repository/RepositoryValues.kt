package pe.edu.hzg.tesoreria.data.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToLong

internal fun temporaryId(): Int {
    val positive = UUID.randomUUID().hashCode() and Int.MAX_VALUE
    return -positive.coerceAtLeast(1)
}

internal fun utcIsoNow(now: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(now))

internal fun Double.toCents(): Long = (this * 100.0).roundToLong()
internal fun Long.fromCents(): Double = this / 100.0
