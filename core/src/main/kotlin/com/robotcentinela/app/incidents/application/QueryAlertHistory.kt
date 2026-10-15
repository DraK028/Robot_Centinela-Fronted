package com.robotcentinela.app.incidents.application

import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.incidents.domain.Alert
import com.robotcentinela.app.incidents.domain.AlertState
import com.robotcentinela.app.incidents.ports.AlertFilter
import com.robotcentinela.app.incidents.ports.AlertQueryPort
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shared.UtcInterval
import java.time.LocalDate

/**
 * Caso de uso: consultar el historial de alertas por fechas y tipo de evento (HU07, RF16).
 *
 * - Convierte las fechas locales a UTC con la zona del domicilio seleccionado (HU07 CDA 1).
 * - Solo consulta robots del domicilio actual (HU07 CDA 2). El servidor lo garantiza además con RLS.
 */
class QueryAlertHistory(
    private val port: AlertQueryPort,
    private val currentScope: CurrentHomeScope,
) {

    /**
     * @param from primer día local (inclusive); `null` junto con [toInclusive] = sin límite temporal.
     * @param toInclusive último día local (inclusive).
     */
    suspend operator fun invoke(
        robotId: RobotId,
        page: PageRequest,
        from: LocalDate? = null,
        toInclusive: LocalDate? = null,
        types: Set<String> = emptySet(),
        severities: Set<String> = emptySet(),
        states: Set<AlertState> = emptySet(),
    ): List<Alert> {
        val scope = currentScope.requireRobot(robotId)
        require((from == null) == (toInclusive == null)) { "El rango de fechas debe tener inicio y fin" }
        val interval = if (from != null && toInclusive != null) {
            UtcInterval.fromLocalDates(from, toInclusive, scope.home.timezone)
        } else {
            null
        }
        return port.alerts(AlertFilter(robotId, interval, types, severities, states), page)
    }
}
