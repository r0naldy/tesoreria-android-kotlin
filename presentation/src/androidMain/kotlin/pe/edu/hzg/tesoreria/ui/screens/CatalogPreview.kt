package pe.edu.hzg.tesoreria.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import pe.edu.hzg.tesoreria.ui.theme.TesoreriaTheme
import pe.edu.hzg.tesoreria.presentation.CatalogUiState

@Preview(name = "Programas · móvil", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Programas · tableta", showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
private fun CatalogPreview() {
    TesoreriaTheme {
        CatalogContent(
            "Programas de estudio", "Organiza las carreras y su disponibilidad para matrícula.", "programa", Icons.Rounded.School,
            listOf(CatalogItem(1, "Apoyo administrativo", true, "Programa de estudio"), CatalogItem(2, "Mantenimiento de sistemas electrónicos", false, "Programa de estudio")),
            CatalogUiState(), true, false, {}, {}, {}, {}, {},
        )
    }
}
