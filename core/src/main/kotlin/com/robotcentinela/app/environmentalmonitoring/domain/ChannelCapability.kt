package com.robotcentinela.app.environmentalmonitoring.domain

import kotlin.time.Duration

/**
 * Entrada del catálogo de capacidades de un robot (RF05: "sensores instalados, gas objetivo,
 * unidad, límites del método y estado de calibración").
 *
 * Los campos siguen las entidades `channels`, `metric_types` y `units` del expediente (§13.2). El
 * contrato definitivo del endpoint de capacidades está pendiente (ver docs/pendientes.md).
 *
 * @property metricType semántica del canal (`metric_types.code`).
 * @property unitCode unidad registrada del canal (`units.code`), por ejemplo `adc_count` o `ppm`.
 * @property gasTarget gas de interés, si aplica. Registrar un gas objetivo no acredita una
 * capacidad química certificada (§13).
 * @property expectedPeriod periodo esperado entre muestras (`channels.period_ms`).
 * @property enabled si el canal está habilitado en el registro.
 */
data class ChannelCapability(
    val channelKey: ChannelKey,
    val metricType: String,
    val unitCode: String?,
    val gasTarget: String?,
    val expectedPeriod: Duration?,
    val enabled: Boolean,
)
