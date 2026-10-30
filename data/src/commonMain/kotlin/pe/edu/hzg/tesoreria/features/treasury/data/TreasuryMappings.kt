package pe.edu.hzg.tesoreria.features.treasury.data

import pe.edu.hzg.tesoreria.features.treasury.domain.model.*

fun TreasuryDashboardDto.toDomain(): TreasuryDashboard = TreasuryDashboard(
    totalRecaudadoHoy = totalRecaudadoHoy,
    totalRecaudadoMes = totalRecaudadoMes,
    ticketsHoy = ticketsHoy,
    ticketsMes = ticketsMes,
    totalRecaudadoAyer = totalRecaudadoAyer,
    totalRecaudadoMesPasado = totalRecaudadoMesPasado,
    ticketsAyer = ticketsAyer,
    ticketsMesPasado = ticketsMesPasado,
    programasActivos = programasActivos,
    serviciosMasCobrados = serviciosMasCobrados.map { it.toDomain() },
    programasConMasPagos = programasConMasPagos.map { it.toDomain() },
    ultimosTickets = ultimosTickets.map { it.toDomain() },
    ingresosDiarios = ingresosDiarios.map { it.toDomain() },
)

fun TreasuryDashboard.toDto(): TreasuryDashboardDto = TreasuryDashboardDto(
    totalRecaudadoHoy = totalRecaudadoHoy,
    totalRecaudadoMes = totalRecaudadoMes,
    ticketsHoy = ticketsHoy,
    ticketsMes = ticketsMes,
    totalRecaudadoAyer = totalRecaudadoAyer,
    totalRecaudadoMesPasado = totalRecaudadoMesPasado,
    ticketsAyer = ticketsAyer,
    ticketsMesPasado = ticketsMesPasado,
    programasActivos = programasActivos,
    serviciosMasCobrados = serviciosMasCobrados.map { it.toDto() },
    programasConMasPagos = programasConMasPagos.map { it.toDto() },
    ultimosTickets = ultimosTickets.map { it.toDto() },
    ingresosDiarios = ingresosDiarios.map { it.toDto() },
)

fun DashboardPeriodDto.toDomain(): DashboardPeriod = DashboardPeriod(
    hayPeriodoActivo = hayPeriodoActivo,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    ingresosAcumulados = ingresosAcumulados,
    tickets = tickets,
    estudiantes = estudiantes,
    servicios = servicios,
    diasRestantes = diasRestantes,
)

fun DashboardPeriod.toDto(): DashboardPeriodDto = DashboardPeriodDto(
    hayPeriodoActivo = hayPeriodoActivo,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    ingresosAcumulados = ingresosAcumulados,
    tickets = tickets,
    estudiantes = estudiantes,
    servicios = servicios,
    diasRestantes = diasRestantes,
)

fun CountItemDto.toDomain(): CountItem = CountItem(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun CountItem.toDto(): CountItemDto = CountItemDto(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun DailyIncomeDto.toDomain(): DailyIncome = DailyIncome(
    fecha = fecha,
    monto = monto,
)

fun DailyIncome.toDto(): DailyIncomeDto = DailyIncomeDto(
    fecha = fecha,
    monto = monto,
)

fun StudentLookupDto.toDomain(): StudentLookup = StudentLookup(
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

fun StudentLookup.toDto(): StudentLookupDto = StudentLookupDto(
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

fun CreatedStudentDto.toDomain(): CreatedStudent = CreatedStudent(
    id = id,
)

fun CreatedStudent.toDto(): CreatedStudentDto = CreatedStudentDto(
    id = id,
)

fun TreasuryProgramDto.toDomain(): TreasuryProgram = TreasuryProgram(
    id = id,
    nombre = nombre,
    estado = estado,
    fechaCreacion = fechaCreacion,
)

fun TreasuryProgram.toDto(): TreasuryProgramDto = TreasuryProgramDto(
    id = id,
    nombre = nombre,
    estado = estado,
    fechaCreacion = fechaCreacion,
)

fun ShiftDto.toDomain(): Shift = Shift(
    id = id,
    nombre = nombre,
)

fun Shift.toDto(): ShiftDto = ShiftDto(
    id = id,
    nombre = nombre,
)

fun TreasuryServiceDto.toDomain(): TreasuryService = TreasuryService(
    id = id,
    nombre = nombre,
    precio = precio,
    estado = estado,
)

fun TreasuryService.toDto(): TreasuryServiceDto = TreasuryServiceDto(
    id = id,
    nombre = nombre,
    precio = precio,
    estado = estado,
)

fun ActivePeriodDto.toDomain(): ActivePeriod = ActivePeriod(
    id = id,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado,
    diasRestantes = diasRestantes,
)

fun ActivePeriod.toDto(): ActivePeriodDto = ActivePeriodDto(
    id = id,
    nombre = nombre,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado,
    diasRestantes = diasRestantes,
)

fun EnrollmentRegisterDto.toDomain(): EnrollmentRegister = EnrollmentRegister(
    id = id,
    periodoId = periodoId,
    periodo = periodo,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    cantidad = cantidad,
)

fun EnrollmentRegister.toDto(): EnrollmentRegisterDto = EnrollmentRegisterDto(
    id = id,
    periodoId = periodoId,
    periodo = periodo,
    programaId = programaId,
    programa = programa,
    turnoId = turnoId,
    turno = turno,
    seccion = seccion,
    cantidad = cantidad,
)

fun EmitTicketDetailRequest.toDomain(): EmitTicketDetailCommand = EmitTicketDetailCommand(
    servicioId = servicioId,
    importe = importe,
)

fun EmitTicketDetailCommand.toDto(): EmitTicketDetailRequest = EmitTicketDetailRequest(
    servicioId = servicioId,
    importe = importe,
)

fun EmitTicketRequest.toDomain(): EmitTicketCommand = EmitTicketCommand(
    alumnoId = alumnoId,
    detalles = detalles.map { it.toDomain() },
)

fun EmitTicketCommand.toDto(): EmitTicketRequest = EmitTicketRequest(
    alumnoId = alumnoId,
    detalles = detalles.map { it.toDto() },
)

fun TicketDetailDto.toDomain(): TicketDetail = TicketDetail(
    servicioId = servicioId,
    servicio = servicio,
    importe = importe,
)

fun TicketDetail.toDto(): TicketDetailDto = TicketDetailDto(
    servicioId = servicioId,
    servicio = servicio,
    importe = importe,
)

fun TicketDto.toDomain(): Ticket = Ticket(
    id = id,
    numeroTicket = numeroTicket,
    contador = contador,
    fechaEmision = fechaEmision,
    dni = dni,
    alumnoNombre = alumnoNombre,
    programa = programa,
    turno = turno,
    total = total,
    totalEnLetras = totalEnLetras,
    usuario = usuario,
    estado = estado,
    detalles = detalles.map { it.toDomain() },
)

fun Ticket.toDto(): TicketDto = TicketDto(
    id = id,
    numeroTicket = numeroTicket,
    contador = contador,
    fechaEmision = fechaEmision,
    dni = dni,
    alumnoNombre = alumnoNombre,
    programa = programa,
    turno = turno,
    total = total,
    totalEnLetras = totalEnLetras,
    usuario = usuario,
    estado = estado,
    detalles = detalles.map { it.toDto() },
)

fun TicketListItemDto.toDomain(): TicketListItem = TicketListItem(
    id = id,
    numeroTicket = numeroTicket,
    contador = contador,
    fechaEmision = fechaEmision,
    dni = dni,
    alumnoNombre = alumnoNombre,
    total = total,
    usuario = usuario,
    estado = estado,
)

fun TicketListItem.toDto(): TicketListItemDto = TicketListItemDto(
    id = id,
    numeroTicket = numeroTicket,
    contador = contador,
    fechaEmision = fechaEmision,
    dni = dni,
    alumnoNombre = alumnoNombre,
    total = total,
    usuario = usuario,
    estado = estado,
)

fun InstitutionConfigDto.toDomain(): InstitutionConfig = InstitutionConfig(
    nombreInstitucion = nombreInstitucion,
    ciudad = ciudad,
    baseLegal = baseLegal,
    tituloComprobante = tituloComprobante,
    tipoComprobante = tipoComprobante,
    logoBase64 = logoBase64,
)

fun InstitutionConfig.toDto(): InstitutionConfigDto = InstitutionConfigDto(
    nombreInstitucion = nombreInstitucion,
    ciudad = ciudad,
    baseLegal = baseLegal,
    tituloComprobante = tituloComprobante,
    tipoComprobante = tipoComprobante,
    logoBase64 = logoBase64,
)

fun IncomeReportDto.toDomain(): IncomeReport = IncomeReport(
    desde = desde,
    hasta = hasta,
    cantidadTickets = cantidadTickets,
    cantidadServicios = cantidadServicios,
    total = total,
    items = items.map { it.toDomain() },
    resumenPorServicio = resumenPorServicio.map { it.toDomain() },
    resumenPorPrograma = resumenPorPrograma.map { it.toDomain() },
)

fun IncomeReport.toDto(): IncomeReportDto = IncomeReportDto(
    desde = desde,
    hasta = hasta,
    cantidadTickets = cantidadTickets,
    cantidadServicios = cantidadServicios,
    total = total,
    items = items.map { it.toDto() },
    resumenPorServicio = resumenPorServicio.map { it.toDto() },
    resumenPorPrograma = resumenPorPrograma.map { it.toDto() },
)

fun IncomeReportItemDto.toDomain(): IncomeReportItem = IncomeReportItem(
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

fun IncomeReportItem.toDto(): IncomeReportItemDto = IncomeReportItemDto(
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

fun ReportSummaryDto.toDomain(): ReportSummary = ReportSummary(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun ReportSummary.toDto(): ReportSummaryDto = ReportSummaryDto(
    nombre = nombre,
    cantidad = cantidad,
    monto = monto,
)

fun ExpenseListItemDto.toDomain(): ExpenseListItem = ExpenseListItem(
    id = id,
    proveedor = proveedor,
    ruc = ruc,
    tipoDocumento = tipoDocumento,
    numeroComprobante = numeroComprobante,
    fechaEmision = fechaEmision,
    fechaRegistro = fechaRegistro,
    total = total,
    moneda = moneda,
    categoria = categoria,
    concepto = concepto,
    confianza = confianza,
    esDuplicadoProbable = esDuplicadoProbable,
    usuario = usuario,
    estado = estado,
)

fun ExpenseListItem.toDto(): ExpenseListItemDto = ExpenseListItemDto(
    id = id,
    proveedor = proveedor,
    ruc = ruc,
    tipoDocumento = tipoDocumento,
    numeroComprobante = numeroComprobante,
    fechaEmision = fechaEmision,
    fechaRegistro = fechaRegistro,
    total = total,
    moneda = moneda,
    categoria = categoria,
    concepto = concepto,
    confianza = confianza,
    esDuplicadoProbable = esDuplicadoProbable,
    usuario = usuario,
    estado = estado,
)

fun ExpenseProductDto.toDomain(): ExpenseProduct = ExpenseProduct(
    orden = orden,
    descripcion = descripcion,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
    importe = importe,
)

fun ExpenseProduct.toDto(): ExpenseProductDto = ExpenseProductDto(
    orden = orden,
    descripcion = descripcion,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
    importe = importe,
)

fun ExpenseDto.toDomain(): Expense = Expense(
    id = id,
    proveedor = proveedor,
    ruc = ruc,
    tipoDocumento = tipoDocumento,
    numeroComprobante = numeroComprobante,
    fechaEmision = fechaEmision,
    horaEmision = horaEmision,
    moneda = moneda,
    subtotal = subtotal,
    igv = igv,
    total = total,
    categoria = categoria,
    concepto = concepto,
    metodoPago = metodoPago,
    confianza = confianza,
    esDuplicadoProbable = esDuplicadoProbable,
    observaciones = observaciones,
    imagenRuta = imagenRuta,
    imagenUrl = imagenUrl,
    imagenBase64 = imagenBase64,
    fechaRegistro = fechaRegistro,
    usuario = usuario,
    estado = estado,
    cantidadConceptos = cantidadConceptos,
    productos = productos.map { it.toDomain() },
)

fun Expense.toDto(): ExpenseDto = ExpenseDto(
    id = id,
    proveedor = proveedor,
    ruc = ruc,
    tipoDocumento = tipoDocumento,
    numeroComprobante = numeroComprobante,
    fechaEmision = fechaEmision,
    horaEmision = horaEmision,
    moneda = moneda,
    subtotal = subtotal,
    igv = igv,
    total = total,
    categoria = categoria,
    concepto = concepto,
    metodoPago = metodoPago,
    confianza = confianza,
    esDuplicadoProbable = esDuplicadoProbable,
    observaciones = observaciones,
    imagenRuta = imagenRuta,
    imagenUrl = imagenUrl,
    imagenBase64 = imagenBase64,
    fechaRegistro = fechaRegistro,
    usuario = usuario,
    estado = estado,
    cantidadConceptos = cantidadConceptos,
    productos = productos.map { it.toDto() },
)
