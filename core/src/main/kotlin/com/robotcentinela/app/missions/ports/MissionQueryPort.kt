package com.robotcentinela.app.missions.ports

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.missions.domain.MissionSummary
import com.robotcentinela.app.shared.PageRequest

/**
 * Puerto de solo lectura de misiones (RF14, RF33).
 *
 * **PROVISIONAL.** El contrato está pendiente (docs/pendientes.md). Hoy solo lo implementa el
 * simulador de datos (HU15).
 */
interface MissionQueryPort {

    /** Misiones del robot, de la más reciente a la más antigua por inicio. */
    suspend fun missions(robotId: RobotId, page: PageRequest): List<MissionSummary>

    /** Resumen de una misión, o `null` si no existe o no es visible. */
    suspend fun mission(robotId: RobotId, missionId: MissionId): MissionSummary?
}
