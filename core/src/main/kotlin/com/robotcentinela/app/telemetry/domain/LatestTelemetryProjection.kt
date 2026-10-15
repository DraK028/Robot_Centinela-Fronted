package com.robotcentinela.app.telemetry.domain

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.SensorReading
import java.time.Instant

/**
 * Marca de captura de un frame. Sirve para decidir qué muestra es más reciente.
 *
 * Criterio (§7.4: "La selección de último dato compara tiempo de captura confiable y sesión"):
 * 1. Mismo dispositivo y mismo boot: manda `sequence`, que es el orden de captura del firmware.
 * 2. Distinto boot, ambos con reloj sincronizado: manda `captured_at`.
 * 3. En otro caso: se usa `received_at` como aproximación explícita
 *    ([timeIsApproximate] = `true`).
 */
data class CaptureStamp(
    val frameId: FrameId,
    val streamVersion: StreamVersion,
    val deviceId: DeviceId,
    val bootId: BootId,
    val sequence: Long,
    val reliableCapturedAt: Instant?,
    val receivedAt: Instant,
) {
    /** `true` si no hay hora de captura confiable y se usa `received_at` (§7.4). */
    val timeIsApproximate: Boolean get() = reliableCapturedAt == null

    /** Instante usado para calcular la edad del dato (HU04). */
    val referenceInstant: Instant get() = reliableCapturedAt ?: receivedAt

    /** Indica si esta captura es posterior a [other] según el criterio de la clase. */
    fun isNewerThan(other: CaptureStamp): Boolean {
        if (deviceId == other.deviceId && bootId == other.bootId) {
            return sequence > other.sequence
        }
        val mine = reliableCapturedAt
        val theirs = other.reliableCapturedAt
        val comparison = if (mine != null && theirs != null) {
            mine.compareTo(theirs)
        } else {
            receivedAt.compareTo(other.receivedAt)
        }
        return comparison > 0 || (comparison == 0 && streamVersion > other.streamVersion)
    }

    companion object {
        fun of(frame: TelemetryFrame) = CaptureStamp(
            frameId = frame.id,
            streamVersion = frame.streamVersion,
            deviceId = frame.deviceId,
            bootId = frame.bootId,
            sequence = frame.sequence,
            reliableCapturedAt = frame.reliableCapturedAt,
            receivedAt = frame.receivedAt,
        )
    }
}

/**
 * Última muestra conocida de un canal. Conserva su propia marca de captura y su frame (§7.4: "cada
 * tarjeta conserva su captured_at y frame_id").
 */
data class ChannelSample(val reading: SensorReading, val stamp: CaptureStamp)

/**
 * Proyección del último estado conocido de un robot, por canal y por frame (RF06, RF09).
 *
 * Reglas:
 * - Una muestra atrasada (por ejemplo, un frame enviado tras una desconexión) entra al historial,
 *   pero **no** reemplaza un dato más reciente. El dashboard no retrocede (RB19, RF09).
 * - Cada canal guarda su propia hora. La cabecera no atribuye una sola hora a todo (§7.4).
 * - [lastReceivedAt] es el `received_at` más reciente de cualquier frame aplicado. Se usa para
 *   detectar una ingesta interrumpida (RNF04).
 *
 * Es inmutable: [apply] devuelve una proyección nueva.
 */
data class LatestTelemetryProjection(
    val channels: Map<ChannelKey, ChannelSample> = emptyMap(),
    val latestFrame: TelemetryFrame? = null,
    val lastReceivedAt: Instant? = null,
) {

    /** Incorpora un frame persistido y devuelve la proyección resultante. */
    fun apply(frame: TelemetryFrame): LatestTelemetryProjection {
        val stamp = CaptureStamp.of(frame)

        val updatedChannels = channels.toMutableMap()
        for (reading in frame.readings) {
            val current = updatedChannels[reading.channelKey]
            if (current == null || stamp.isNewerThan(current.stamp)) {
                updatedChannels[reading.channelKey] = ChannelSample(reading, stamp)
            }
        }

        val currentLatest = latestFrame
        val newLatest = if (currentLatest == null || stamp.isNewerThan(CaptureStamp.of(currentLatest))) {
            frame
        } else {
            currentLatest
        }

        val previousReceived = lastReceivedAt
        val newLastReceived = if (previousReceived == null || frame.receivedAt > previousReceived) {
            frame.receivedAt
        } else {
            previousReceived
        }

        return LatestTelemetryProjection(updatedChannels, newLatest, newLastReceived)
    }
}
