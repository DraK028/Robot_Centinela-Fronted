package com.robotcentinela.app.environmentalmonitoring.adapters.demo

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelCapability
import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.MetricAggregate
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingQuality
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingValue
import com.robotcentinela.app.environmentalmonitoring.ports.CapabilitiesPort
import com.robotcentinela.app.environmentalmonitoring.ports.MetricAggregatePort
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.shell.demo.DemoWorld
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Catálogo de capacidades simulado (RF05). Las unidades y gases objetivo son los del ejemplo del
 * expediente: los MQ reportan señal cruda en cuentas ADC, nunca ppm (RB05).
 */
class DemoCapabilitiesAdapter(private val world: DemoWorld) : CapabilitiesPort {

    override suspend fun capabilitiesOf(robotId: RobotId): List<ChannelCapability> {
        world.requireOnline("capacidades")
        if (robotId != world.robot.id) return emptyList()
        val period = 2.seconds
        return listOf(
            ChannelCapability(DemoWorld.MQ6, "gas_signal", "adc_count", "LPG", period, enabled = true),
            ChannelCapability(DemoWorld.MQ4, "gas_signal", "adc_count", "CH4", period, enabled = true),
            ChannelCapability(DemoWorld.TEMPERATURE, "temperature", "celsius", null, period, enabled = true),
            ChannelCapability(DemoWorld.HUMIDITY, "relative_humidity", "percent", null, period, enabled = true),
            ChannelCapability(DemoWorld.MQ7, "gas_signal", "adc_count", "CO", period, enabled = false),
            ChannelCapability(DemoWorld.FRONT_DISTANCE, "distance", "m", null, period, enabled = true),
            ChannelCapability(DemoWorld.IR_FRONT_LEFT, "floor_presence", "boolean", null, period, enabled = true),
            ChannelCapability(DemoWorld.IR_FRONT_RIGHT, "floor_presence", "boolean", null, period, enabled = true),
            ChannelCapability(DemoWorld.IR_REAR_LEFT, "floor_presence", "boolean", null, period, enabled = true),
            ChannelCapability(DemoWorld.IR_REAR_RIGHT, "floor_presence", "boolean", null, period, enabled = true),
        )
    }
}

/**
 * Agregados simulados (HU07 CDA 3). En producción los calcula el servidor (§13.4); aquí se calculan
 * sobre el historial sintético con la misma regla: los huecos se conservan (intervalos sin muestras
 * válidas tienen mínimo, máximo y promedio nulos, RF10).
 */
class DemoMetricAggregateAdapter(private val world: DemoWorld) : MetricAggregatePort {

    override suspend fun aggregates(
        robotId: RobotId,
        channelKey: ChannelKey?,
        interval: UtcInterval,
        bucketSize: Duration,
    ): List<MetricAggregate> {
        world.requireOnline("agregados")
        if (robotId != world.robot.id || channelKey == null) return emptyList()
        val bucketMs = bucketSize.inWholeMilliseconds
        val startMs = interval.start.toEpochMilli()
        val bucketCount = ((interval.endExclusive.toEpochMilli() - startMs + bucketMs - 1) / bucketMs).toInt()

        val valid = Array(bucketCount) { mutableListOf<Double>() }
        val invalid = LongArray(bucketCount)
        world.framesBetween(interval, missionId = null).forEach { frame ->
            val reading = frame.readings.firstOrNull { it.channelKey == channelKey } ?: return@forEach
            val at = (frame.capturedAt ?: frame.receivedAt).toEpochMilli()
            val bucket = ((at - startMs) / bucketMs).toInt().coerceIn(0, bucketCount - 1)
            val value = (reading.value as? ReadingValue.Numeric)?.value
            val usable = reading.quality == ReadingQuality.Valid ||
                reading.quality == ReadingQuality.Estimated ||
                reading.quality == ReadingQuality.Uncalibrated
            if (value != null && usable) valid[bucket] += value else invalid[bucket]++
        }
        return (0 until bucketCount).map { i ->
            val values = valid[i]
            MetricAggregate(
                channelKey = channelKey,
                bucketStart = interval.start.plusMillis(i * bucketMs),
                bucketSize = bucketSize,
                countValid = values.size.toLong(),
                countInvalid = invalid[i],
                min = values.minOrNull(),
                max = values.maxOrNull(),
                avg = if (values.isEmpty()) null else values.average(),
            )
        }
    }
}
