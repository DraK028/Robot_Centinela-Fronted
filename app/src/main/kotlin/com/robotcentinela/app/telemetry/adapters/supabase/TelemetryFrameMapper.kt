package com.robotcentinela.app.telemetry.adapters.supabase

import com.robotcentinela.app.environmentalmonitoring.domain.CalibrationId
import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingQuality
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingValue
import com.robotcentinela.app.environmentalmonitoring.domain.SensorReading
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.homemapping.domain.Pose
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.robothealth.domain.BatteryStatus
import com.robotcentinela.app.robothealth.domain.RobotReportedState
import com.robotcentinela.app.shared.ContractViolationException
import com.robotcentinela.app.shell.supabase.SupabaseClientFactory
import com.robotcentinela.app.telemetry.domain.BootId
import com.robotcentinela.app.telemetry.domain.ClockQuality
import com.robotcentinela.app.telemetry.domain.DeviceId
import com.robotcentinela.app.telemetry.domain.FrameId
import com.robotcentinela.app.telemetry.domain.FrameRecord
import com.robotcentinela.app.telemetry.domain.StreamVersion
import com.robotcentinela.app.telemetry.domain.TelemetryFrame
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Traduce filas de frames (contrato `JSON_estructura.txt` + campos del backend) al dominio.
 *
 * Reglas del contrato que aplica:
 * - Solo acepta `schema_version` con versión mayor 1. Los cambios aditivos se ignoran; un cambio de
 *   significado crearía v2 con su propio adaptador (§14.2).
 * - Identificadores UUID válidos y timestamps RFC3339 (§7.2).
 * - Como mucho un tipo de valor por lectura y un `channel_key` por frame (§7.2, §13.2).
 * - Un `null` se conserva como ausencia: nunca se convierte en cero ni en `false` (RB04, §13.3).
 *
 * Un frame que no cumple el contrato se devuelve como [FrameRecord.Rejected] con una causa legible
 * que no incluye el payload (RNF19). Así no bloquea el cursor del flujo.
 */
object TelemetryFrameMapper {

    /** Versión mayor del contrato soportada por esta app. */
    const val SUPPORTED_SCHEMA_MAJOR = 1

    /**
     * Convierte una fila de Supabase en un registro del flujo.
     *
     * @throws ContractViolationException si la fila no trae un `stream_version` válido. Sin cursor no
     * se puede ubicar el frame en el flujo.
     */
    fun fromRow(row: JsonObject): FrameRecord {
        val version = row["stream_version"]?.jsonPrimitive?.longOrNull
            ?.takeIf { it >= 0 }
            ?: throw ContractViolationException("La fila de frame no trae un stream_version válido")
        val streamVersion = StreamVersion(version)
        val frameId = row["event_id"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { FrameId(UUID.fromString(it)) }.getOrNull() }

        return try {
            val dto = SupabaseClientFactory.json.decodeFromJsonElement(TelemetryFrameRowDto.serializer(), row)
            FrameRecord.Interpreted(toDomain(dto, streamVersion))
        } catch (e: SerializationException) {
            FrameRecord.Rejected(streamVersion, frameId, "El frame no cumple la estructura del contrato v1")
        } catch (e: FrameContractException) {
            FrameRecord.Rejected(streamVersion, frameId, e.message.orEmpty())
        } catch (e: IllegalArgumentException) {
            // Invariantes del dominio (por ejemplo, pose no finita o channel_key repetido).
            FrameRecord.Rejected(streamVersion, frameId, e.message ?: "El frame viola una regla del dominio")
        }
    }

    /** Convierte un DTO completo al dominio. */
    internal fun toDomain(dto: TelemetryFrameRowDto, streamVersion: StreamVersion): TelemetryFrame {
        val major = dto.schemaVersion.substringBefore('.').toIntOrNull()
        if (major != SUPPORTED_SCHEMA_MAJOR) {
            throw FrameContractException("schema_version ${dto.schemaVersion} no soportada")
        }
        val receivedAt = dto.receivedAt?.let { instant(it, "received_at") }
            ?: throw FrameContractException("Falta received_at asignado por el backend")

        return TelemetryFrame(
            id = FrameId(uuid(dto.eventId, "event_id")),
            schemaVersion = dto.schemaVersion,
            deviceId = DeviceId(uuid(dto.deviceId, "device_id")),
            bootId = BootId(uuid(dto.bootId, "boot_id")),
            sequence = dto.sequence,
            capturedAt = dto.capturedAt?.let { instant(it, "captured_at") },
            monotonicMs = dto.monotonicMs,
            clockQuality = ClockQuality.fromRaw(dto.clockQuality),
            missionId = dto.missionId?.let { MissionId(uuid(it, "mission_id")) },
            mapVersionId = dto.mapVersionId?.let { MapVersionId(uuid(it, "map_version_id")) },
            pose = dto.pose?.let { Pose(it.xM, it.yM, it.thetaRad, it.sigmaPositionM, it.quality) },
            readings = dto.readings.map(::toReading),
            battery = dto.battery?.let { BatteryStatus(it.voltageV, it.socPct, it.currentA, it.quality) },
            robotState = dto.robotState?.let(::RobotReportedState),
            motorsActive = dto.motorsActive,
            firmwareVersion = dto.firmwareVersion,
            receivedAt = receivedAt,
            streamVersion = streamVersion,
        )
    }

    private fun toReading(dto: ReadingDto): SensorReading {
        val numeric = dto.value
        val logical = dto.booleanValue
        if (numeric != null && logical != null) {
            throw FrameContractException("La lectura ${dto.channelKey} trae value y boolean_value a la vez")
        }
        val value = when {
            numeric != null -> ReadingValue.Numeric(numeric)
            logical != null -> ReadingValue.BooleanValue(logical)
            else -> ReadingValue.Absent
        }
        return SensorReading(
            channelKey = ChannelKey(dto.channelKey),
            value = value,
            quality = ReadingQuality.fromRaw(dto.quality),
            calibrationId = dto.calibrationId?.let { CalibrationId(uuid(it, "calibration_id")) },
        )
    }

    private fun uuid(value: String, field: String): UUID = try {
        UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
        throw FrameContractException("$field no es un UUID válido")
    }

    /** Acepta RFC3339 con `Z` o con desplazamiento, como lo devuelve PostgreSQL (`+00:00`). */
    private fun instant(value: String, field: String): Instant = try {
        OffsetDateTime.parse(value).toInstant()
    } catch (e: DateTimeParseException) {
        throw FrameContractException("$field no es un timestamp RFC3339 válido")
    }

    /** Incumplimiento del contrato en un frame concreto. */
    private class FrameContractException(message: String) : Exception(message)
}
