package pe.edu.hzg.tesoreria.features.academic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.hzg.tesoreria.features.academic.domain.model.Student
import pe.edu.hzg.tesoreria.features.academic.domain.model.UpdateStudentCommand
import pe.edu.hzg.tesoreria.features.academic.domain.studentPersonalDataError

@Composable
internal fun EditStudentDialog(
    student: Student,
    busy: Boolean,
    error: String?,
    onSave: (UpdateStudentCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    var names by remember(student.id) { mutableStateOf(student.nombres) }
    var surnames by remember(student.id) { mutableStateOf(student.apellidos) }
    var sex by remember(student.id) { mutableStateOf(student.sexo.orEmpty()) }
    var birthDate by remember(student.id) { mutableStateOf(student.fechaNacimiento?.substringBefore('T').orEmpty()) }
    var phone by remember(student.id) { mutableStateOf(student.celular.orEmpty()) }
    val validation = studentPersonalDataError(names, surnames, birthDate, phone)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Editar estudiante") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DNI ${student.dni} · ${student.programa}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Corrige sus datos personales. El DNI, la carrera y su historial no cambian.", style = MaterialTheme.typography.bodySmall)
                error?.let { FeedbackCard(it, error = true) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(names, { names = it.take(150) }, Modifier.fillMaxWidth(),
                    enabled = !busy, label = { Text("Nombres") }, singleLine = true)
                OutlinedTextField(surnames, { surnames = it.take(150) }, Modifier.fillMaxWidth(),
                    enabled = !busy, label = { Text("Apellidos") }, singleLine = true)
                ChoiceField("Sexo", sex, listOf("" to "Sin registrar", "H" to "Masculino", "M" to "Femenino"),
                    onSelect = { if (!busy) sex = it })
                OutlinedTextField(birthDate, { birthDate = it.take(10) }, Modifier.fillMaxWidth(),
                    enabled = !busy, label = { Text("Nacimiento (AAAA-MM-DD)") }, singleLine = true)
                OutlinedTextField(phone, { phone = it.take(20) }, Modifier.fillMaxWidth(),
                    enabled = !busy, label = { Text("Celular (opcional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !busy && validation == null, onClick = {
                onSave(UpdateStudentCommand(student.id, names.trim(), surnames.trim(), sex.ifBlank { null },
                    birthDate.ifBlank { null }, phone.trim().ifBlank { null }))
            }) { Text(if (busy) "Guardando…" else "Guardar cambios") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } },
    )
}
