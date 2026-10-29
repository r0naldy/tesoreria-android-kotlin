package pe.edu.hzg.tesoreria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pe.edu.hzg.tesoreria.ui.theme.InstitutionalNavy
import pe.edu.hzg.tesoreria.ui.theme.InstitutionalNavyDeep

@Composable
fun GradientHero(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(InstitutionalNavyDeep, InstitutionalNavy, Color(0xFF1A4658)),
                ),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = .88f), style = MaterialTheme.typography.bodyMedium)
        }
        trailing?.let { Box(modifier = Modifier.align(Alignment.CenterEnd)) { it() } }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = .14f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.height(14.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SyncBadge(pending: Long, modifier: Modifier = Modifier) {
    val synced = pending == 0L
    Row(
        modifier = modifier
            .background(
                color = if (synced) Color(0xFFE0F7ED) else Color(0xFFFFF0D5),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (synced) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = if (synced) Color(0xFF138A5D) else Color(0xFFB56A00),
        )
        Text(
            text = if (synced) "Sincronizado" else "$pending pendiente(s)",
            style = MaterialTheme.typography.labelMedium,
            color = if (synced) Color(0xFF096B47) else Color(0xFF855000),
        )
    }
}

@Composable
fun LoadingOverlay(visible: Boolean) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
