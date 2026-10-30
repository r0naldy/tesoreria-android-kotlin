package pe.edu.hzg.tesoreria.features.treasury.ui

import pe.edu.hzg.tesoreria.platform.PlatformContext
import pe.edu.hzg.tesoreria.features.treasury.domain.model.InstitutionConfig
import pe.edu.hzg.tesoreria.features.treasury.domain.model.Ticket

expect object TicketOutput {
    fun share(context: PlatformContext, ticket: Ticket)
    fun print(context: PlatformContext, ticket: Ticket, config: InstitutionConfig? = null)
}
