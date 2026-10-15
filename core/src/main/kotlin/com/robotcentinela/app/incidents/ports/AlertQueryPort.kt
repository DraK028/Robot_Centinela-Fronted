package com.robotcentinela.app.incidents.ports

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.incidents.domain.Alert
import com.robotcentinela.app.incidents.domain.AlertState
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shared.UtcInterval

/**
 * Filtros del historial de alertas (RF16: "fecha, tipo, severidad, robot y estado").
 *
 * Un conjunto vacío significa "sin filtro" para ese campo.
 *
 * @property interval periodo en UTC sobre `opened_at`; `null` = sin límite temporal.
 */
data class AlertFilter(
    val robotId: RobotId,
    val interval: UtcInterval? = null,
    val types: Set<String> = emptySet(),
    val severities: Set<String> = emptySet(),
    val states: Set<AlertState> = emptySet(),
)

/** Puerto de solo lectura de alertas persistidas (RF15, RF16, RB25: el lector no modifica). */
interface AlertQueryPort {

    /** Alertas que cumplen [filter], de la más reciente a la más antigua por `opened_at`. */
    suspend fun alerts(filter: AlertFilter, page: PageRequest): List<Alert>
}
