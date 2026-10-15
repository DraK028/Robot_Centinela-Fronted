package com.robotcentinela.app.environmentalmonitoring.domain

import java.time.Instant
import kotlin.time.Duration

/**
 * Agregado de un canal en un intervalo (`metric_aggregates`, §13.2), para la vista de estadísticas
 * mínimo, máximo y promedio (HU07 CDA 3).
 *
 * Los agregados se calculan en el backend o en SQL, no en el cliente. Así se evita que cada app
 * descargue miles de filas y obtenga promedios distintos (§13.4).
 *
 * @property countValid muestras válidas del intervalo.
 * @property countInvalid muestras no válidas. Los huecos se conservan y no se rellenan (RF10).
 * @property min mínimo del intervalo; `null` si no hubo muestras válidas.
 * @property max máximo del intervalo; `null` si no hubo muestras válidas.
 * @property avg promedio del intervalo; `null` si no hubo muestras válidas.
 */
data class MetricAggregate(
    val channelKey: ChannelKey,
    val bucketStart: Instant,
    val bucketSize: Duration,
    val countValid: Long,
    val countInvalid: Long,
    val min: Double?,
    val max: Double?,
    val avg: Double?,
)
