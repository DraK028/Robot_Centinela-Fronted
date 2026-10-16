package com.robotcentinela.app.homemapping.application

import com.robotcentinela.app.homemapping.domain.FloorPlan
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.homemapping.ports.FloorPlanPort
import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.domain.RobotId

/**
 * Caso de uso: obtener el plano de la versión exacta de mapa de un frame o de una misión (HU05 CDA 2).
 *
 * Solo consulta robots del domicilio seleccionado (HU01 CDA 3). Nunca sustituye la versión pedida
 * por otra más reciente: las posiciones solo tienen sentido en su geometría (RB13).
 */
class QueryFloorPlan(
    private val port: FloorPlanPort,
    private val currentScope: CurrentHomeScope,
) {

    suspend operator fun invoke(robotId: RobotId, mapVersionId: MapVersionId): FloorPlan? {
        currentScope.requireRobot(robotId)
        return port.floorPlan(robotId, mapVersionId)?.takeIf { it.mapVersionId == mapVersionId }
    }
}
