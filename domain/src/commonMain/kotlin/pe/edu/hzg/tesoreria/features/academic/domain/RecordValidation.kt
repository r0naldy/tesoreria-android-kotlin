package pe.edu.hzg.tesoreria.features.academic.domain

import pe.edu.hzg.tesoreria.features.academic.domain.model.UpdateRecordCommand

fun recordPersonalDataError(request: UpdateRecordCommand): String? = when {
    request.id <= 0 -> "El registro no tiene un identificador válido."
    request.profesor.orEmpty().trim().length > 150 -> "El docente admite hasta 150 caracteres."
    request.moduloFormativo.orEmpty().trim().length > 200 -> "El módulo formativo admite hasta 200 caracteres."
    request.resolucionPrograma.orEmpty().trim().length > 150 -> "La resolución admite hasta 150 caracteres."
    else -> null
}
