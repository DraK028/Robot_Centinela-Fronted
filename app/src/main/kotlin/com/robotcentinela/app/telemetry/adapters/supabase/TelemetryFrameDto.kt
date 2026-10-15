package com.robotcentinela.app.telemetry.adapters.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO de un frame. Replica **exactamente** la estructura de `JSON_estructura.txt`
 * (`schema_version` 1.1) más los campos que asigna el backend al persistir (§7.2).
 *
 * Los nombres JSON se conservan tal cual (snake_case). El significado de cada campo está en
 * `docs/data_dictionary/frame-telemetria-v1.md`.
 */
@Serializable
data class TelemetryFrameRowDto(
    @SerialName("schema_version") val schemaVersion: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("boot_id") val bootId: String,
    val sequence: Long,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("monotonic_ms") val monotonicMs: Long,
    @SerialName("clock_quality") val clockQuality: String,
    @SerialName("mission_id") val missionId: String? = null,
    @SerialName("map_version_id") val mapVersionId: String? = null,
    val pose: PoseDto? = null,
    val readings: List<ReadingDto>,
    val battery: BatteryDto? = null,
    @SerialName("robot_state") val robotState: String? = null,
    @SerialName("motors_active") val motorsActive: Boolean? = null,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    // Campos asignados por el backend (§7.2: "El backend asigna received_at y stream_version").
    // PROVISIONAL: su nombre y ubicación dependen del contrato de backend. Son opcionales en el DTO
    // para poder leer también el payload del dispositivo tal cual.
    @SerialName("received_at") val receivedAt: String? = null,
    @SerialName("stream_version") val streamVersion: Long? = null,
)

/** `pose` de `JSON_estructura.txt`. */
@Serializable
data class PoseDto(
    @SerialName("x_m") val xM: Double,
    @SerialName("y_m") val yM: Double,
    @SerialName("theta_rad") val thetaRad: Double,
    @SerialName("sigma_position_m") val sigmaPositionM: Double? = null,
    val quality: String,
)

/**
 * Elemento de `readings` de `JSON_estructura.txt`. Trae `value` (numérico) **o** `boolean_value`
 * (lógico), nunca ambos.
 */
@Serializable
data class ReadingDto(
    @SerialName("channel_key") val channelKey: String,
    val value: Double? = null,
    @SerialName("boolean_value") val booleanValue: Boolean? = null,
    val quality: String,
    @SerialName("calibration_id") val calibrationId: String? = null,
)

/** `battery` de `JSON_estructura.txt`. */
@Serializable
data class BatteryDto(
    @SerialName("voltage_v") val voltageV: Double? = null,
    @SerialName("soc_pct") val socPct: Double? = null,
    @SerialName("current_a") val currentA: Double? = null,
    val quality: String,
)
