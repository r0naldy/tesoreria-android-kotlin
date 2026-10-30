package pe.edu.hzg.tesoreria.features.academic.data

import pe.edu.hzg.tesoreria.features.academic.domain.model.*

fun AcademicProgramDto.toDomain(): AcademicProgram = AcademicProgram(
    id = id,
    nombre = nombre,
    estado = estado,
)

fun AcademicProgram.toDto(): AcademicProgramDto = AcademicProgramDto(
    id = id,
    nombre = nombre,
    estado = estado,
)

fun ShiftDto.toDomain(): Shift = Shift(
    id = id,
    nombre = nombre,
)

fun Shift.toDto(): ShiftDto = ShiftDto(
    id = id,
    nombre = nombre,
)

fun StudentCareerDto.toDomain(): StudentCareer = StudentCareer(
    alumnoId = alumnoId,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    periodo = periodo,
    fechaRegistro = fechaRegistro,
)

fun StudentCareer.toDto(): StudentCareerDto = StudentCareerDto(
    alumnoId = alumnoId,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    periodo = periodo,
    fechaRegistro = fechaRegistro,
)

fun DniLookupDto.toDomain(): DniLookup = DniLookup(
    dni = dni,
    existe = existe,
    encontradoReniec = encontradoReniec,
    alumnoId = alumnoId,
    nombres = nombres,
    apellidos = apellidos,
    nombreCompleto = nombreCompleto,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    celular = celular,
    seccion = seccion,
    carreras = carreras.map { it.toDomain() },
)

fun DniLookup.toDto(): DniLookupDto = DniLookupDto(
    dni = dni,
    existe = existe,
    encontradoReniec = encontradoReniec,
    alumnoId = alumnoId,
    nombres = nombres,
    apellidos = apellidos,
    nombreCompleto = nombreCompleto,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    celular = celular,
    seccion = seccion,
    carreras = carreras.map { it.toDto() },
)

fun StudentDto.toDomain(): Student = Student(
    id = id,
    dni = dni,
    nombres = nombres,
    apellidos = apellidos,
    nombreCompleto = nombreCompleto,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    celular = celular,
    estado = estado,
    fechaRegistro = fechaRegistro,
    pagado = pagado,
    ratificadoEl = ratificadoEl,
    semestreDeRegistro = semestreDeRegistro,
    semestreRatificado = semestreRatificado,
    fechaRetiro = fechaRetiro,
    motivoRetiro = motivoRetiro,
    usuarioRetiro = usuarioRetiro,
)

fun Student.toDto(): StudentDto = StudentDto(
    id = id,
    dni = dni,
    nombres = nombres,
    apellidos = apellidos,
    nombreCompleto = nombreCompleto,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    celular = celular,
    estado = estado,
    fechaRegistro = fechaRegistro,
    pagado = pagado,
    ratificadoEl = ratificadoEl,
    semestreDeRegistro = semestreDeRegistro,
    semestreRatificado = semestreRatificado,
    fechaRetiro = fechaRetiro,
    motivoRetiro = motivoRetiro,
    usuarioRetiro = usuarioRetiro,
)

fun CreateStudentRequest.toDomain(): CreateStudentCommand = CreateStudentCommand(
    dni = dni,
    nombres = nombres,
    apellidos = apellidos,
    programaId = programaId,
    turnoId = turnoId,
    seccion = seccion,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    celular = celular,
    condicion = condicion,
)

fun CreateStudentCommand.toDto(): CreateStudentRequest = CreateStudentRequest(
    dni = dni,
    nombres = nombres,
    apellidos = apellidos,
    programaId = programaId,
    turnoId = turnoId,
    seccion = seccion,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    celular = celular,
    condicion = condicion,
)

fun CreatedIdDto.toDomain(): CreatedId = CreatedId(
    id = id,
)

fun CreatedId.toDto(): CreatedIdDto = CreatedIdDto(
    id = id,
)

fun UpdateStudentRequest.toDomain(): UpdateStudentCommand = UpdateStudentCommand(
    id = id,
    nombres = nombres,
    apellidos = apellidos,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    celular = celular,
)

fun UpdateStudentCommand.toDto(): UpdateStudentRequest = UpdateStudentRequest(
    id = id,
    nombres = nombres,
    apellidos = apellidos,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    celular = celular,
)

fun RatificationDto.toDomain(): Ratification = Ratification(
    alumnoId = alumnoId,
    semestre = semestre,
    ratificadoEl = ratificadoEl,
)

fun Ratification.toDto(): RatificationDto = RatificationDto(
    alumnoId = alumnoId,
    semestre = semestre,
    ratificadoEl = ratificadoEl,
)

fun WithdrawalRequest.toDomain(): WithdrawalCommand = WithdrawalCommand(
    motivo = motivo,
    detalle = detalle,
    devolucion = devolucion,
    metodoDevolucion = metodoDevolucion,
)

fun WithdrawalCommand.toDto(): WithdrawalRequest = WithdrawalRequest(
    motivo = motivo,
    detalle = detalle,
    devolucion = devolucion,
    metodoDevolucion = metodoDevolucion,
)

fun WithdrawalResultDto.toDomain(): WithdrawalResult = WithdrawalResult(
    alumnoId = alumnoId,
    pagado = pagado,
    devuelto = devuelto,
    egresoId = egresoId,
)

fun WithdrawalResult.toDto(): WithdrawalResultDto = WithdrawalResultDto(
    alumnoId = alumnoId,
    pagado = pagado,
    devuelto = devuelto,
    egresoId = egresoId,
)

fun SemesterDto.toDomain(): Semester = Semester(
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    cerrado = cerrado,
)

fun Semester.toDto(): SemesterDto = SemesterDto(
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    cerrado = cerrado,
)

fun PeriodDto.toDomain(): Period = Period(
    id = id,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado,
    usuarioApertura = usuarioApertura,
    fechaApertura = fechaApertura,
    usuarioCierre = usuarioCierre,
    fechaCierre = fechaCierre,
    observaciones = observaciones,
    tickets = tickets,
    ingresos = ingresos,
    diasRestantes = diasRestantes,
    semestres = semestres.map { it.toDomain() },
    semestreActivo = semestreActivo?.toDomain(),
)

fun Period.toDto(): PeriodDto = PeriodDto(
    id = id,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado,
    usuarioApertura = usuarioApertura,
    fechaApertura = fechaApertura,
    usuarioCierre = usuarioCierre,
    fechaCierre = fechaCierre,
    observaciones = observaciones,
    tickets = tickets,
    ingresos = ingresos,
    diasRestantes = diasRestantes,
    semestres = semestres.map { it.toDto() },
    semestreActivo = semestreActivo?.toDto(),
)

fun PeriodSummaryDto.toDomain(): PeriodSummary = PeriodSummary(
    id = id,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado,
    tickets = tickets,
    estudiantes = estudiantes,
    ingresos = ingresos,
    servicios = servicios,
    usuarioApertura = usuarioApertura,
    usuarioCierre = usuarioCierre,
    fechaCierre = fechaCierre,
)

fun PeriodSummary.toDto(): PeriodSummaryDto = PeriodSummaryDto(
    id = id,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado,
    tickets = tickets,
    estudiantes = estudiantes,
    ingresos = ingresos,
    servicios = servicios,
    usuarioApertura = usuarioApertura,
    usuarioCierre = usuarioCierre,
    fechaCierre = fechaCierre,
)

fun OpenPeriodRequest.toDomain(): OpenPeriodCommand = OpenPeriodCommand(
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    observaciones = observaciones,
)

fun OpenPeriodCommand.toDto(): OpenPeriodRequest = OpenPeriodRequest(
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    observaciones = observaciones,
)

fun IncomeItemDto.toDomain(): IncomeItem = IncomeItem(
    fecha = fecha,
    numeroTicket = numeroTicket,
    contador = contador,
    dni = dni,
    alumno = alumno,
    programa = programa,
    servicio = servicio,
    importe = importe,
    usuario = usuario,
)

fun IncomeItem.toDto(): IncomeItemDto = IncomeItemDto(
    fecha = fecha,
    numeroTicket = numeroTicket,
    contador = contador,
    dni = dni,
    alumno = alumno,
    programa = programa,
    servicio = servicio,
    importe = importe,
    usuario = usuario,
)

fun SummaryGroupDto.toDomain(): SummaryGroup = SummaryGroup(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun SummaryGroup.toDto(): SummaryGroupDto = SummaryGroupDto(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun PeriodDetailDto.toDomain(): PeriodDetail = PeriodDetail(
    periodo = periodo.toDomain(),
    items = items.map { it.toDomain() },
    total = total,
    resumenPorServicio = resumenPorServicio.map { it.toDomain() },
    resumenPorPrograma = resumenPorPrograma.map { it.toDomain() },
    resumenPorUsuario = resumenPorUsuario.map { it.toDomain() },
)

fun PeriodDetail.toDto(): PeriodDetailDto = PeriodDetailDto(
    periodo = periodo.toDto(),
    items = items.map { it.toDto() },
    total = total,
    resumenPorServicio = resumenPorServicio.map { it.toDto() },
    resumenPorPrograma = resumenPorPrograma.map { it.toDto() },
    resumenPorUsuario = resumenPorUsuario.map { it.toDto() },
)

fun RecordDto.toDomain(): Record = Record(
    id = id,
    periodoId = periodoId,
    periodo = periodo,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    cantidad = cantidad,
    fechaCreacion = fechaCreacion,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
    resolucionPrograma = resolucionPrograma,
)

fun Record.toDto(): RecordDto = RecordDto(
    id = id,
    periodoId = periodoId,
    periodo = periodo,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    cantidad = cantidad,
    fechaCreacion = fechaCreacion,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
    resolucionPrograma = resolucionPrograma,
)

fun UpdateRecordRequest.toDomain(): UpdateRecordCommand = UpdateRecordCommand(
    id = id,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
    resolucionPrograma = resolucionPrograma,
)

fun UpdateRecordCommand.toDto(): UpdateRecordRequest = UpdateRecordRequest(
    id = id,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
    resolucionPrograma = resolucionPrograma,
)

fun CreateRecordRequest.toDomain(): CreateRecordCommand = CreateRecordCommand(
    programaId = programaId,
    turnoId = turnoId,
    seccion = seccion,
    periodoId = periodoId,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
)

fun CreateRecordCommand.toDto(): CreateRecordRequest = CreateRecordRequest(
    programaId = programaId,
    turnoId = turnoId,
    seccion = seccion,
    periodoId = periodoId,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
)

fun EnrollmentItemDto.toDomain(): EnrollmentItem = EnrollmentItem(
    n = n,
    codigoMatricula = codigoMatricula,
    apellidosNombres = apellidosNombres,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    celular = celular,
    condicion = condicion,
)

fun EnrollmentItem.toDto(): EnrollmentItemDto = EnrollmentItemDto(
    n = n,
    codigoMatricula = codigoMatricula,
    apellidosNombres = apellidosNombres,
    sexo = sexo,
    fechaNacimiento = fechaNacimiento,
    edad = edad,
    celular = celular,
    condicion = condicion,
)

fun EnrollmentRosterDto.toDomain(): EnrollmentRoster = EnrollmentRoster(
    nombreInstitucion = nombreInstitucion,
    nombreCetpro = nombreCetpro,
    codigoModular = codigoModular,
    direccion = direccion,
    ciudad = ciudad,
    periodo = periodo,
    periodoInicio = periodoInicio,
    periodoFin = periodoFin,
    programa = programa,
    turno = turno,
    seccion = seccion,
    semestre = semestre,
    dreGre = dreGre,
    ugel = ugel,
    resolucionCreacion = resolucionCreacion,
    resolucionAutorizacion = resolucionAutorizacion,
    periodoLectivo = periodoLectivo,
    modalidadServicio = modalidadServicio,
    nivelFormativo = nivelFormativo,
    tipoPlan = tipoPlan,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
    cantidad = cantidad,
    estudiantes = estudiantes.map { it.toDomain() },
    hombres = hombres,
    mujeres = mujeres,
    pagantes = pagantes,
    becados = becados,
)

fun EnrollmentRoster.toDto(): EnrollmentRosterDto = EnrollmentRosterDto(
    nombreInstitucion = nombreInstitucion,
    nombreCetpro = nombreCetpro,
    codigoModular = codigoModular,
    direccion = direccion,
    ciudad = ciudad,
    periodo = periodo,
    periodoInicio = periodoInicio,
    periodoFin = periodoFin,
    programa = programa,
    turno = turno,
    seccion = seccion,
    semestre = semestre,
    dreGre = dreGre,
    ugel = ugel,
    resolucionCreacion = resolucionCreacion,
    resolucionAutorizacion = resolucionAutorizacion,
    periodoLectivo = periodoLectivo,
    modalidadServicio = modalidadServicio,
    nivelFormativo = nivelFormativo,
    tipoPlan = tipoPlan,
    profesor = profesor,
    moduloFormativo = moduloFormativo,
    cantidad = cantidad,
    estudiantes = estudiantes.map { it.toDto() },
    hombres = hombres,
    mujeres = mujeres,
    pagantes = pagantes,
    becados = becados,
)

fun ComparisonDayDto.toDomain(): ComparisonDay = ComparisonDay(
    fecha = fecha,
    ingresos = ingresos,
    egresos = egresos,
)

fun ComparisonDay.toDto(): ComparisonDayDto = ComparisonDayDto(
    fecha = fecha,
    ingresos = ingresos,
    egresos = egresos,
)

fun ComparisonGroupDto.toDomain(): ComparisonGroup = ComparisonGroup(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun ComparisonGroup.toDto(): ComparisonGroupDto = ComparisonGroupDto(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun MovementDto.toDomain(): Movement = Movement(
    tipo = tipo,
    fecha = fecha,
    descripcion = descripcion,
    detalle = detalle,
    monto = monto,
    usuario = usuario,
)

fun Movement.toDto(): MovementDto = MovementDto(
    tipo = tipo,
    fecha = fecha,
    descripcion = descripcion,
    detalle = detalle,
    monto = monto,
    usuario = usuario,
)

fun ComparisonDto.toDomain(): Comparison = Comparison(
    desde = desde,
    hasta = hasta,
    totalIngresos = totalIngresos,
    totalEgresos = totalEgresos,
    balance = balance,
    cantidadIngresos = cantidadIngresos,
    cantidadEgresos = cantidadEgresos,
    serie = serie.map { it.toDomain() },
    egresosPorCategoria = egresosPorCategoria.map { it.toDomain() },
    ingresosPorConcepto = ingresosPorConcepto.map { it.toDomain() },
    movimientos = movimientos.map { it.toDomain() },
)

fun Comparison.toDto(): ComparisonDto = ComparisonDto(
    desde = desde,
    hasta = hasta,
    totalIngresos = totalIngresos,
    totalEgresos = totalEgresos,
    balance = balance,
    cantidadIngresos = cantidadIngresos,
    cantidadEgresos = cantidadEgresos,
    serie = serie.map { it.toDto() },
    egresosPorCategoria = egresosPorCategoria.map { it.toDto() },
    ingresosPorConcepto = ingresosPorConcepto.map { it.toDto() },
    movimientos = movimientos.map { it.toDto() },
)
