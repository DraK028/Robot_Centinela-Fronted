package com.robotcentinela.app.incidents.adapters.demo

import com.robotcentinela.app.incidents.domain.Alert
import com.robotcentinela.app.incidents.ports.AlertFilter
import com.robotcentinela.app.incidents.ports.AlertQueryPort
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shell.demo.DemoWorld

/**
 * Alertas simuladas. Igual que en producción, la app no calcula alertas: el simulador entrega
 * incidentes ya persistidos y la app solo los filtra y pagina (RB22, RB23, RB25).
 */
class DemoAlertQueryAdapter(private val world: DemoWorld) : AlertQueryPort {

    override suspend fun alerts(filter: AlertFilter, page: PageRequest): List<Alert> {
        world.requireOnline("historial de alertas")
        return world.alerts().asSequence()
            .filter { it.robotId == filter.robotId }
            .filter { filter.interval == null || it.openedAt in filter.interval!! }
            .filter { filter.types.isEmpty() || it.type in filter.types }
            .filter { filter.severities.isEmpty() || it.severity in filter.severities }
            .filter { filter.states.isEmpty() || it.state in filter.states }
            .drop(page.offset.toInt())
            .take(page.limit)
            .toList()
    }
}
