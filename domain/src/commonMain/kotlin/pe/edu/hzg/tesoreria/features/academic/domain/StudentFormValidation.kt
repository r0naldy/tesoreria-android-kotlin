package pe.edu.hzg.tesoreria.features.academic.domain

import pe.edu.hzg.tesoreria.domain.util.*
import kotlinx.datetime.LocalDate

fun studentPersonalDataError(
    names: String,
    surnames: String,
    birthDate: String,
    phone: String,
    today: LocalDate = LocalDate.now(),
): String? = when {
    names.trim().isEmpty() -> "Ingresa los nombres del estudiante."
    surnames.trim().isEmpty() -> "Ingresa los apellidos del estudiante."
    names.trim().length > 150 || surnames.trim().length > 150 -> "Nombres y apellidos admiten hasta 150 caracteres cada uno."
    birthDate.isNotBlank() && runCatching { LocalDate.parse(birthDate) }.getOrNull() == null -> "La fecha debe ser válida y tener el formato AAAA-MM-DD."
    birthDate.isNotBlank() && LocalDate.parse(birthDate).isAfter(today) -> "La fecha de nacimiento no puede estar en el futuro."
    birthDate.isNotBlank() && LocalDate.parse(birthDate).year < 1900 -> "La fecha de nacimiento debe ser de 1900 en adelante."
    phone.isNotBlank() && (phone.length > 20 || !phone.matches(Regex("[+() 0-9-]+")) || phone.count(Char::isDigit) < 7) -> "Ingresa un teléfono válido de hasta 20 caracteres o déjalo vacío."
    else -> null
}
