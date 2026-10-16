package com.robotcentinela.app.environmentalmonitoring.application

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelCapability
import com.robotcentinela.app.environmentalmonitoring.ports.CapabilitiesPort
import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.domain.RobotId

/**
 * Caso de uso: obtener el catálogo de capacidades de un robot del domicilio seleccionado (RF05).
 *
 * Con el catálogo, [DescribeChannelStatus] conoce la unidad, el gas objetivo y el periodo de cada canal,
 * e incluye como "Sin dato" los canales registrados que todavía no reportan.
 */
class QueryChannelCapabilities(
    private val port: CapabilitiesPort,
    private val currentScope: CurrentHomeScope,
) {

    suspend operator fun invoke(robotId: RobotId): List<ChannelCapability> {
        currentScope.requireRobot(robotId)
        return port.capabilitiesOf(robotId)
    }
}
