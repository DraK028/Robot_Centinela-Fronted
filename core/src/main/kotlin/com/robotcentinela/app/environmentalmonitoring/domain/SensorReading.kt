package com.robotcentinela.app.environmentalmonitoring.domain

import java.util.UUID

/**
 * Clave de canal (`readings[].channel_key`), por ejemplo `mq6_raw`, `temperature` o
 * `ir_floor_front_left`.
 *
 * Su significado y su unidad se resuelven en el registro del dispositivo, no en el frame (§7.2:
 * "la unidad no se acepta libremente del emisor").
 */
@JvmInline
value class ChannelKey(val value: String)

/** Calibración aplicada a una lectura (`readings[].calibration_id`; `calibrations.id`, §13.2). */
@JvmInline
value class CalibrationId(val value: UUID)

/**
 * Valor de una lectura. Un frame trae `value` numérico **o** `boolean_value`, nunca ambos
 * (§13.2: "a lo sumo un tipo de valor").
 */
sealed interface ReadingValue {

    /** Valor numérico finito, en la unidad del canal. */
    data class Numeric(val value: Double) : ReadingValue {
        init {
            require(value.isFinite()) { "Una lectura numérica debe ser finita (sin NaN ni Infinity)" }
        }
    }

    /**
     * Valor lógico. Por ejemplo, en los IR de piso, `true` = piso detectado y `false` = riesgo de
     * borde (§7.2). La polaridad se calibra por módulo en el firmware.
     */
    data class BooleanValue(val value: Boolean) : ReadingValue

    /**
     * Sin valor (`null` en el JSON). **Nunca** equivale a cero ni a `false` (RB04, §13.3: "bool null
     * no se transforma en false").
     */
    data object Absent : ReadingValue
}

/**
 * Lectura de un canal dentro de un frame (`readings[]`; `sensor_readings`, §13.2).
 *
 * El histórico es inmutable: una recalibración posterior no cambia esta lectura (HU09 CDA 2, RB07).
 */
data class SensorReading(
    val channelKey: ChannelKey,
    val value: ReadingValue,
    val quality: ReadingQuality,
    val calibrationId: CalibrationId?,
)
