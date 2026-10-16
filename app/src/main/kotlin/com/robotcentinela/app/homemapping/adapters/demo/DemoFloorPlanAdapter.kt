package com.robotcentinela.app.homemapping.adapters.demo

import com.robotcentinela.app.homemapping.domain.FloorPlan
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.homemapping.ports.FloorPlanPort
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.shell.demo.DemoPlans
import com.robotcentinela.app.shell.demo.DemoWorld

/** Planos sintéticos por versión de mapa (HU05, HU19). */
class DemoFloorPlanAdapter(private val world: DemoWorld) : FloorPlanPort {

    override suspend fun floorPlan(robotId: RobotId, mapVersionId: MapVersionId): FloorPlan? {
        world.requireOnline("plano")
        if (robotId != world.robot.id) return null
        return DemoPlans.floorPlan(mapVersionId)
    }
}
