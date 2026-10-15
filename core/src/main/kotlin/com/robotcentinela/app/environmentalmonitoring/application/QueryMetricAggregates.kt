package com.robotcentinela.app.environmentalmonitoring.application

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.MetricAggregate
import com.robotcentinela.app.environmentalmonitoring.ports.MetricAggregatePort
import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.shared.UtcInterval
import java.time.LocalDate
import kotlin.time.Duration

/**
 * Caso de uso: obtener estadísticas resumidas (mínimo, máximo, promedio) de un intervalo local
 * (HU07 CDA 3, RF10).
 *
 * Los agregados los calcula el servidor (§13.4). El adaptador de [MetricAggregatePort] está
 * pendiente (docs/pendientes.md).
 */
class QueryMetricAggregates(
    private val port: MetricAggregatePort,
    private val currentScope: CurrentHomeScope,
) {

    suspend operator fun invoke(
        robotId: RobotId,
        from: LocalDate,
        toInclusive: LocalDate,
        bucketSize: Duration,
        channelKey: ChannelKey? = null,
    ): List<MetricAggregate> {
        require(bucketSize.isPositive()) { "El tamaño del intervalo de agregación debe ser positivo" }
        val scope = currentScope.requireRobot(robotId)
        val interval = UtcInterval.fromLocalDates(from, toInclusive, scope.home.timezone)
        return port.aggregates(robotId, channelKey, interval, bucketSize)
    }
}
