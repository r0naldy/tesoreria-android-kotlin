package pe.edu.hzg.tesoreria.features.academic.ui

import pe.edu.hzg.tesoreria.platform.currencyText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import pe.edu.hzg.tesoreria.platform.platformContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import pe.edu.hzg.tesoreria.features.academic.AcademicShareFile

fun money(value: Double): String = currencyText(value)

fun shortDate(value: String?): String = value
    ?.substringBefore('T')
    ?.split('-')
    ?.takeIf { it.size == 3 }
    ?.let { "${it[2]}/${it[1]}/${it[0]}" }
    ?: "—"

@Composable
fun AcademicHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onRefresh: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer),
                    ),
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(11.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            onRefresh?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    val background = if (positive) Color(0xFFDDF7EA) else Color(0xFFFFE4E6)
    val foreground = if (positive) Color(0xFF08734B) else Color(0xFFB4233C)
    Text(
        text = text,
        modifier = modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        color = foreground,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
fun FeedbackCard(message: String, error: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer else Color(0xFFDFF7EC),
        ),
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF075F43),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun ChoiceField(
    label: String,
    value: String,
    choices: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    choices.firstOrNull { it.first == value }?.second ?: "Seleccionar",
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { (key, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(key)
                        },
                    )
                }
            }
        }
    }
}

@Composable
expect fun CollectAcademicShares(files: SharedFlow<AcademicShareFile>)
