package pe.edu.hzg.tesoreria.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AdminHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onRefresh: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            .background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer)),
                RoundedCornerShape(24.dp),
            ).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                .padding(11.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        onRefresh?.let {
            IconButton(onClick = it) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar")
            }
        }
    }
}

@Composable
internal fun AdminAccessDenied(section: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("Acceso restringido", style = MaterialTheme.typography.titleLarge)
        Text(
            "$section está disponible únicamente para usuarios con rol Administrador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun StatusPill(
    text: String,
    positive: Boolean,
) {
    val background = if (positive) Color(0xFFE0F7ED) else MaterialTheme.colorScheme.errorContainer
    val foreground = if (positive) Color(0xFF08784F) else MaterialTheme.colorScheme.onErrorContainer
    Text(
        text = text,
        modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
    )
}

@Composable
internal fun AdminMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
