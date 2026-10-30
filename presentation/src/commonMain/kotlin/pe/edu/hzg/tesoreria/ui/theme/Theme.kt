package pe.edu.hzg.tesoreria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = InstitutionalTeal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF073D40),
    secondary = SteelBlue,
    secondaryContainer = Color(0xFFE4EDF6),
    onSecondaryContainer = InstitutionalNavy,
    tertiary = MintTertiary,
    tertiaryContainer = Color(0xFFDDF4E7),
    onTertiaryContainer = Color(0xFF14532D),
    background = LightBackground,
    onBackground = Color(0xFF16182B),
    surface = Color.White,
    onSurface = Color(0xFF16182B),
    surfaceVariant = Color(0xFFEEF0FA),
    onSurfaceVariant = Color(0xFF565A75),
    outline = Color(0xFF85899F),
    outlineVariant = Color(0xFFE4E6F2),
    error = Color(0xFFB91C1C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5D1),
    onPrimary = Color(0xFF073638),
    primaryContainer = Color(0xFF164C51),
    onPrimaryContainer = Color(0xFFD0F6F2),
    secondary = Color(0xFFA9C8F1),
    secondaryContainer = Color(0xFF263D5C),
    onSecondaryContainer = Color(0xFFE2EEFF),
    tertiary = Color(0xFF7DDAA4),
    tertiaryContainer = Color(0xFF15462B),
    onTertiaryContainer = Color(0xFFB1F2C7),
    background = DarkBackground,
    onBackground = Color(0xFFE8EDF7),
    surface = Color(0xFF17243A),
    onSurface = Color(0xFFE8EDF7),
    surfaceVariant = Color(0xFF23334B),
    onSurfaceVariant = Color(0xFFB5C2D5),
    outline = Color(0xFF8796AC),
    outlineVariant = Color(0xFF3E4C63),
)

@Composable
fun TesoreriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TesoreriaTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
