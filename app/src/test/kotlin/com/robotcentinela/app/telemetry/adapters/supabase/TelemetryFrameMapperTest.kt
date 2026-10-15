package com.robotcentinela.app.telemetry.adapters.supabase

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingQuality
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingValue
import com.robotcentinela.app.shared.ContractViolationException
import com.robotcentinela.app.shell.supabase.SupabaseClientFactory
import com.robotcentinela.app.telemetry.domain.ClockQuality
import com.robotcentinela.app.telemetry.domain.FrameRecord
import com.robotcentinela.app.telemetry.domain.StreamVersion
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrato del frame según `JSON_estructura.txt` (§7.2, §13.3, RB04, RNF12) y fixtures QA (HU15).
 */
class TelemetryFrameMapperTest {

    private fun fixture(name: String): JsonObject {
        val text = checkNotNull(javaClass.getResource("/fixtures/telemetry/$name")) { "Falta el fixture $name" }
            .readText()
        return SupabaseClientFactory.json.parseToJsonElement(text).jsonObject
    }

    /** El payload del dispositivo más los campos que asigna el backend al persistir. */
    private fun persistedRow(
        payload: JsonObject = fixture("frame_v1_1_device_payload.json"),
        receivedAt: String? = "2026-10-11T15:24:13.000Z",
        streamVersion: Long? = 42,
    ) = JsonObject(
        payload + buildMap {
            receivedAt?.let { put("received_at", JsonPrimitive(it)) }
            streamVersion?.let { put("stream_version", JsonPrimitive(it)) }
        },
    )

    private fun JsonObject.with(key: String, value: kotlinx.serialization.json.JsonElement) = JsonObject(this + (key to value))

    @Test
    fun `el DTO lee exactamente el payload de JSON_estructura`() {
        val dto = SupabaseClientFactory.json.decodeFromJsonElement(
            TelemetryFrameRowDto.serializer(),
            fixture("frame_v1_1_device_payload.json"),
        )
        assertEquals("1.1", dto.schemaVersion)
        assertEquals(10, dto.readings.size)
        assertNull(dto.receivedAt)
        assertNull(dto.streamVersion)
    }

    @Test
    fun `un frame persistido valido se traduce completo al dominio`() {
        val record = assertIs<FrameRecord.Interpreted>(TelemetryFrameMapper.fromRow(persistedRow()))
        val frame = record.frame

        assertEquals(UUID.fromString("9f225860-42f2-4cd0-8c3a-e4e970395307"), frame.id.value)
        assertEquals(1843, frame.sequence)
        assertEquals(Instant.parse("2026-10-11T15:24:12Z"), frame.capturedAt)
        assertEquals(929450, frame.monotonicMs)
        assertEquals(ClockQuality.Synced, frame.clockQuality)
        assertEquals(StreamVersion(42), frame.streamVersion)
        assertEquals(Instant.parse("2026-10-11T15:24:13Z"), frame.receivedAt)
        assertEquals(0.08, frame.pose?.sigmaPositionM)
        assertEquals("moving", frame.robotState?.raw)
        assertEquals(true, frame.motorsActive)
        assertEquals("0.2.0", frame.firmwareVersion)
        assertEquals(11.4, frame.battery?.voltageV)
        assertNull(frame.battery?.socPct)
        assertEquals("voltage_only", frame.battery?.quality)

        val readings = frame.readings.associateBy { it.channelKey }
        assertEquals(ReadingValue.Numeric(1860.0), readings.getValue(ChannelKey("mq6_raw")).value)
        assertEquals(ReadingQuality.Uncalibrated, readings.getValue(ChannelKey("mq6_raw")).quality)
        assertEquals(ReadingValue.Absent, readings.getValue(ChannelKey("mq7_raw")).value)
        assertEquals(ReadingQuality.NotInstalled, readings.getValue(ChannelKey("mq7_raw")).quality)
        assertEquals(ReadingValue.BooleanValue(true), readings.getValue(ChannelKey("ir_floor_rear_right")).value)
        assertEquals(ReadingValue.Numeric(0.45), readings.getValue(ChannelKey("front_distance_m")).value)
    }

    @Test
    fun `una fila sin stream_version incumple el contrato`() {
        assertFailsWith<ContractViolationException> { TelemetryFrameMapper.fromRow(persistedRow(streamVersion = null)) }
    }

    @Test
    fun `sin received_at el frame se rechaza sin bloquear el cursor`() {
        val record = assertIs<FrameRecord.Rejected>(TelemetryFrameMapper.fromRow(persistedRow(receivedAt = null)))
        assertEquals(StreamVersion(42), record.streamVersion)
        assertEquals(UUID.fromString("9f225860-42f2-4cd0-8c3a-e4e970395307"), record.frameId?.value)
    }

    @Test
    fun `los estados de sensor se conservan y null nunca se vuelve cero ni false`() {
        val frame = assertIs<FrameRecord.Interpreted>(TelemetryFrameMapper.fromRow(fixture("frame_qa_sensor_states.json"))).frame
        val readings = frame.readings.associateBy { it.channelKey.value }

        assertEquals(ReadingQuality.Warming, readings.getValue("mq6_raw").quality)
        assertEquals(ReadingQuality.Fault, readings.getValue("mq4_raw").quality)
        assertEquals(ReadingValue.Absent, readings.getValue("temperature").value)
        assertEquals(ReadingQuality.OutOfRange, readings.getValue("humidity").quality)
        assertEquals(ReadingQuality.Unknown("calibrating"), readings.getValue("front_distance_m").quality)
        assertEquals(ReadingValue.Absent, readings.getValue("ir_floor_front_left").value)
        assertEquals(ReadingValue.BooleanValue(false), readings.getValue("ir_floor_front_right").value)
        assertEquals(ClockQuality.Unsynced, frame.clockQuality)
        assertNull(frame.pose)
        assertNull(frame.motorsActive)
        assertEquals(Instant.parse("2026-10-11T15:24:13Z"), frame.receivedAt)
    }

    @Test
    fun `una lectura con value y boolean_value se rechaza`() {
        val record = assertIs<FrameRecord.Rejected>(TelemetryFrameMapper.fromRow(fixture("frame_qa_invalid_both_values.json")))
        assertTrue(record.reason.contains("ir_floor_rear_left"))
    }

    @Test
    fun `una version mayor no soportada se rechaza`() {
        val record = assertIs<FrameRecord.Rejected>(TelemetryFrameMapper.fromRow(fixture("frame_qa_schema_v2.json")))
        assertTrue(record.reason.contains("2.0"))
    }

    @Test
    fun `los campos nuevos aditivos de v1 se ignoran`() {
        val row = persistedRow().with("sample_note", JsonPrimitive("campo futuro"))
        assertIs<FrameRecord.Interpreted>(TelemetryFrameMapper.fromRow(row))
    }

    @Test
    fun `un channel_key repetido se rechaza`() {
        val row = persistedRow()
        val readings = row.getValue("readings").jsonArray
        val duplicated = row.with("readings", JsonArray(readings + readings.first()))
        assertIs<FrameRecord.Rejected>(TelemetryFrameMapper.fromRow(duplicated))
    }

    @Test
    fun `un UUID invalido se rechaza sin incluir el valor recibido`() {
        val row = persistedRow().with("device_id", JsonPrimitive("robot-del-domicilio"))
        val record = assertIs<FrameRecord.Rejected>(TelemetryFrameMapper.fromRow(row))
        assertTrue(record.reason.contains("device_id"))
        assertFalse(record.reason.contains("robot-del-domicilio"))
    }

    @Test
    fun `captured_at nulo se conserva como ausente`() {
        val row = persistedRow().with("captured_at", JsonNull)
        val frame = assertIs<FrameRecord.Interpreted>(TelemetryFrameMapper.fromRow(row)).frame
        assertNull(frame.capturedAt)
        assertNull(frame.reliableCapturedAt)
    }

    @Test
    fun `un frame que no cumple la estructura se rechaza con causa generica`() {
        val row = buildJsonObject {
            put("stream_version", JsonPrimitive(5))
            put("schema_version", JsonPrimitive("1.1"))
        }
        val record = assertIs<FrameRecord.Rejected>(TelemetryFrameMapper.fromRow(row))
        assertEquals(StreamVersion(5), record.streamVersion)
    }
}
