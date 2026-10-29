package pe.edu.hzg.tesoreria.features.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun money(value: Double, currency: String = "PEN"): String = runCatching {
    NumberFormat.getCurrencyInstance(Locale("es", "PE")).apply {
        this.currency = java.util.Currency.getInstance(if (currency == "USD") "USD" else "PEN")
    }.format(value)
}.getOrElse { "S/ %.2f".format(Locale.US, value) }

fun shortDate(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val date = value.take(10).split('-')
    val formatted = if (date.size == 3) "${date[2]}/${date[1]}/${date[0]}" else value.take(10)
    val time = value.substringAfter('T', "").take(5).takeIf { it.length == 5 }
    return if (time == null) formatted else "$formatted $time"
}

fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun monthStartIso(): String {
    val calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

@Composable
fun NoticeCard(message: String?, modifier: Modifier = Modifier, error: Boolean = false) {
    if (message.isNullOrBlank()) return
    val color = if (error) MaterialTheme.colorScheme.error else Color(0xFF147753)
    Row(
        modifier = modifier.fillMaxWidth().background(color.copy(alpha = .10f), RoundedCornerShape(16.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(if (error) Icons.Rounded.ErrorOutline else Icons.Rounded.Info, contentDescription = null, tint = color)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            content()
        }
    }
}
