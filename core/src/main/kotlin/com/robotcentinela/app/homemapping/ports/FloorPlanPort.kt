package com.robotcentinela.app.homemapping.ports

import com.robotcentinela.app.homemapping.domain.FloorPlan
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.identity.domain.RobotId

/**
 * Puerto de consulta de la geometría de una versión de mapa (HU05, RF11, RF32).
 *
 * **PROVISIONAL.** El contrato de mapas (formato y Storage privado con URL firmada) está pendiente
 * (docs/pendientes.md). Hoy solo lo implementa el simulador de datos (HU15).
 */
interface FloorPlanPort {

    /**
     * Plano de la versión [mapVersionId] del robot. `null` si no hay archivo de mapa: la interfaz
     * muestra entonces la lista de estaciones (HU05 CDA 3).
     */
    suspend fun floorPlan(robotId: RobotId, mapVersionId: MapVersionId): FloorPlan?
}
