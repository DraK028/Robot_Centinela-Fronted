package com.robotcentinela.app.telemetry.application

import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.telemetry.domain.FrameRecord
import com.robotcentinela.app.telemetry.ports.TelemetryQueryPort
import java.time.LocalDate

/**
 * Caso de uso: consultar el historial de frames por fechas locales (HU07, RF10).
 *
 * - Convierte el filtro local a UTC con la zona del domicilio (HU07 CDA 1).
 * - Solo consulta robots del domicilio seleccionado (HU07 CDA 2).
 * - Devuelve los frames tal como se persistieron: unidades sin cambios y huecos sin rellenar (RF10).
 *
 * Los filtros por canal y por calidad de RF10 quedan pendientes del contrato definitivo
 * (docs/pendientes.md).
 */
class QueryTelemetryHistory(
    private val query: TelemetryQueryPort,
    private val currentScope: CurrentHomeScope,
) {

    suspend operator fun invoke(
        robotId: RobotId,
        from: LocalDate,
        toInclusive: LocalDate,
        page: PageRequest,
        missionId: MissionId? = null,
    ): List<FrameRecord> {
        val scope = currentScope.requireRobot(robotId)
        val interval = UtcInterval.fromLocalDates(from, toInclusive, scope.home.timezone)
        return query.framesBetween(robotId, interval, missionId, page)
    }
}
