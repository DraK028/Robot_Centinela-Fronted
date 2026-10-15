package com.robotcentinela.app.environmentalmonitoring.ports

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelCapability
import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.MetricAggregate
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.shared.UtcInterval
import kotlin.time.Duration

/**
 * Puerto del catálogo de capacidades (RF05; contrato lógico `GET /v1/robots/{id}/capabilities`, §7.3).
 *
 * Esta versión todavía no tiene adaptador: el contrato del endpoint está pendiente
 * (docs/pendientes.md). Mientras tanto la app interpreta las lecturas sin unidad de catálogo.
 */
interface CapabilitiesPort {
    suspend fun capabilitiesOf(robotId: RobotId): List<ChannelCapability>
}

/**
 * Puerto de agregados calculados en el servidor (HU07 CDA 3, RF10, §13.4).
 *
 * Esta versión todavía no tiene adaptador: el contrato está pendiente (docs/pendientes.md).
 */
interface MetricAggregatePort {
    suspend fun aggregates(
        robotId: RobotId,
        channelKey: ChannelKey?,
        interval: UtcInterval,
        bucketSize: Duration,
    ): List<MetricAggregate>
}
