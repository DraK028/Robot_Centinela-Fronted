package com.robotcentinela.app.missions.application

import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.missions.domain.MissionSummary
import com.robotcentinela.app.missions.ports.MissionQueryPort
import com.robotcentinela.app.shared.PageRequest

/** Caso de uso: listar las misiones de un robot del domicilio seleccionado (RF14, HU01 CDA 3). */
class ListMissions(
    private val port: MissionQueryPort,
    private val currentScope: CurrentHomeScope,
) {

    suspend operator fun invoke(robotId: RobotId, page: PageRequest): List<MissionSummary> {
        currentScope.requireRobot(robotId)
        return port.missions(robotId, page).filter { it.robotId == robotId }
    }
}

/** Caso de uso: obtener el resumen de una misión (HU05, HU06, HU08). */
class GetMissionSummary(
    private val port: MissionQueryPort,
    private val currentScope: CurrentHomeScope,
) {

    suspend operator fun invoke(robotId: RobotId, missionId: MissionId): MissionSummary? {
        currentScope.requireRobot(robotId)
        return port.mission(robotId, missionId)?.takeIf { it.robotId == robotId }
    }
}
