package com.robotcentinela.app.missions.adapters.demo

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.missions.domain.MissionSummary
import com.robotcentinela.app.missions.ports.MissionQueryPort
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shell.demo.DemoWorld

/** Resúmenes de misión simulados (RF14, RF33), con estaciones omitidas y su causa (HU06 CDA 2). */
class DemoMissionAdapter(private val world: DemoWorld) : MissionQueryPort {

    override suspend fun missions(robotId: RobotId, page: PageRequest): List<MissionSummary> {
        world.requireOnline("misiones")
        if (robotId != world.robot.id) return emptyList()
        return world.missions().drop(page.offset.toInt()).take(page.limit)
    }

    override suspend fun mission(robotId: RobotId, missionId: MissionId): MissionSummary? {
        world.requireOnline("misión")
        if (robotId != world.robot.id) return null
        return world.missions().firstOrNull { it.id == missionId }
    }
}
