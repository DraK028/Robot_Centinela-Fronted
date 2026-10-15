package com.robotcentinela.app.telemetry.domain

import com.robotcentinela.app.environmentalmonitoring.domain.SensorReading
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.homemapping.domain.Pose
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.robothealth.domain.BatteryStatus
import com.robotcentinela.app.robothealth.domain.RobotReportedState
import java.time.Instant

/**
 * Frame de telemetría persistido: el conjunto de muestras de un instante, con su identidad y
 * contexto de captura (glosario §19.2).
 *
 * Replica la estructura de `JSON_estructura.txt` (`schema_version` 1.1) más los dos campos que asigna
 * el backend al persistir (§7.2): [receivedAt] y [streamVersion]. El diccionario completo de campos
 * está en `docs/data_dictionary/frame-telemetria-v1.md`.
 *
 * Un frame se presenta completo: no se mezcla la temperatura de uno con el gas de otro sin su marca
 * temporal (§7.4).
 *
 * @property sequence orden de captura dentro de [bootId], asignado por el firmware.
 * @property capturedAt hora de captura del dispositivo; `null` si falta. Nunca la inventa el cliente.
 * @property monotonicMs tiempo relativo del dispositivo, útil cuando el reloj no está sincronizado.
 * @property pose pose estimada; `null` = sin localización. No invalida las lecturas, solo su
 * ubicación (§7.2).
 * @property motorsActive orden o estado reportado. No prueba un giro efectivo sin encoder (§7.0).
 * @property receivedAt instante en que el backend recibió el frame.
 * @property streamVersion cursor de commit por robot.
 */
data class TelemetryFrame(
    val id: FrameId,
    val schemaVersion: String,
    val deviceId: DeviceId,
    val bootId: BootId,
    val sequence: Long,
    val capturedAt: Instant?,
    val monotonicMs: Long,
    val clockQuality: ClockQuality,
    val missionId: MissionId?,
    val mapVersionId: MapVersionId?,
    val pose: Pose?,
    val readings: List<SensorReading>,
    val battery: BatteryStatus?,
    val robotState: RobotReportedState?,
    val motorsActive: Boolean?,
    val firmwareVersion: String?,
    val receivedAt: Instant,
    val streamVersion: StreamVersion,
) {
    init {
        require(sequence >= 0) { "sequence no puede ser negativo" }
        require(monotonicMs >= 0) { "monotonic_ms no puede ser negativo" }
        require(readings.map { it.channelKey }.toSet().size == readings.size) {
            "Un frame no puede repetir channel_key (§7.2)"
        }
    }

    /**
     * Hora de captura confiable: solo existe si el reloj estaba sincronizado y el frame trae
     * `captured_at`.
     */
    val reliableCapturedAt: Instant?
        get() = capturedAt?.takeIf { clockQuality == ClockQuality.Synced }
}
