package com.robotcentinela.app.environmentalmonitoring.application

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelCapability
import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.DataFreshness
import com.robotcentinela.app.environmentalmonitoring.domain.FreshnessPolicy
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingInterpretation
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingInterpreter
import com.robotcentinela.app.telemetry.domain.ChannelSample
import com.robotcentinela.app.telemetry.domain.LatestTelemetryProjection
import java.time.Clock

/**
 * Estado de un canal: los datos que necesita una tarjeta de métrica, sin decidir su presentación
 * (HU02 CDA 3, HU04 CDA 3).
 *
 * @property interpretation qué se puede afirmar de la lectura (HU02, HU20).
 * @property freshness edad y estado de atraso (HU04); `null` si el canal nunca ha reportado.
 * @property sample última muestra con su `captured_at`, `received_at` y `frame_id`; `null` si nunca
 * reportó.
 * @property capability entrada del catálogo, si está disponible (unidad, gas objetivo, periodo).
 */
data class ChannelStatus(
    val channelKey: ChannelKey,
    val interpretation: ReadingInterpretation,
    val freshness: DataFreshness?,
    val sample: ChannelSample?,
    val capability: ChannelCapability?,
)

/**
 * Caso de uso: describir el estado de cada canal a partir de la proyección del último dato
 * (HU02, HU04, HU20).
 *
 * Incluye los canales recibidos y los del catálogo que todavía no han reportado. Estos últimos
 * aparecen como [ReadingInterpretation.NoData], nunca como cero.
 */
class DescribeChannelStatus(
    private val freshnessPolicy: FreshnessPolicy,
    private val clock: Clock,
) {

    operator fun invoke(
        projection: LatestTelemetryProjection,
        capabilities: List<ChannelCapability> = emptyList(),
    ): List<ChannelStatus> {
        val now = clock.instant()
        val catalog = capabilities.associateBy { it.channelKey }
        val keys = LinkedHashSet(projection.channels.keys).apply { addAll(catalog.keys) }

        return keys.map { key ->
            val sample = projection.channels[key]
            val capability = catalog[key]
            if (sample == null) {
                ChannelStatus(key, ReadingInterpretation.NoData, null, null, capability)
            } else {
                ChannelStatus(
                    channelKey = key,
                    interpretation = ReadingInterpreter.interpret(sample.reading, capability),
                    freshness = freshnessPolicy.evaluate(
                        sampleInstant = sample.stamp.referenceInstant,
                        expectedPeriod = capability?.expectedPeriod,
                        now = now,
                    ),
                    sample = sample,
                    capability = capability,
                )
            }
        }
    }
}
