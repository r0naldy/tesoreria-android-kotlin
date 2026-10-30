@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package pe.edu.hzg.tesoreria.data.repository

import pe.edu.hzg.tesoreria.domain.util.nowMillis
import kotlin.uuid.Uuid
import kotlin.math.roundToLong

internal fun temporaryId(): Int {
    val positive = Uuid.random().hashCode() and Int.MAX_VALUE
    return -positive.coerceAtLeast(1)
}

internal fun utcIsoNow(now: Long = nowMillis()): String =
    kotlinx.datetime.Instant.fromEpochMilliseconds(now / 1000 * 1000).toString()

internal fun Double.toCents(): Long = (this * 100.0).roundToLong()
internal fun Long.fromCents(): Double = this / 100.0
