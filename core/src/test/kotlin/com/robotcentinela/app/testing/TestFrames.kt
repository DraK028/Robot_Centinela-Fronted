package com.robotcentinela.app.testing

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingQuality
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingValue
import com.robotcentinela.app.environmentalmonitoring.domain.SensorReading
import com.robotcentinela.app.identity.domain.Home
import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.robothealth.domain.BatteryStatus
import com.robotcentinela.app.robothealth.domain.RobotReportedState
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.telemetry.domain.BootId
import com.robotcentinela.app.telemetry.domain.ClockQuality
import com.robotcentinela.app.telemetry.domain.DeviceId
import com.robotcentinela.app.telemetry.domain.FrameId
import com.robotcentinela.app.telemetry.domain.StreamVersion
import com.robotcentinela.app.telemetry.domain.TelemetryFrame
import java.time.Instant
import java.util.UUID

/**
 * Datos sintéticos para pruebas (RB29: los datos de simulación se identifican como tales).
 * No representan ninguna vivienda ni dispositivo real.
 */
object TestFrames {

    val DEVICE = DeviceId(UUID(0, 0xD1))
    val BOOT_1 = BootId(UUID(0, 0xB1))
    val BOOT_2 = BootId(UUID(0, 0xB2))
    val HOME_ID = HomeId(UUID(0, 0x01))
    val OTHER_HOME_ID = HomeId(UUID(0, 0x02))
    val ROBOT_ID = RobotId(UUID(0, 0x10))
    val OTHER_ROBOT_ID = RobotId(UUID(0, 0x20))
    val T0: Instant = Instant.parse("2026-10-11T15:24:12Z")

    val HOME = Home(HOME_ID, "Domicilio sintético", UtcInterval.DEFAULT_ZONE)
    val ROBOT = Robot(ROBOT_ID, HOME_ID, "sim-01", "active")

    fun reading(
        key: String,
        value: Double?,
        quality: ReadingQuality = ReadingQuality.Valid,
    ) = SensorReading(
        channelKey = ChannelKey(key),
        value = value?.let { ReadingValue.Numeric(it) } ?: ReadingValue.Absent,
        quality = quality,
        calibrationId = null,
    )

    fun frame(
        version: Long,
        sequence: Long = version,
        bootId: BootId = BOOT_1,
        capturedAt: Instant? = T0.plusSeconds(2 * version),
        receivedAt: Instant = T0.plusSeconds(2 * version + 1),
        clockQuality: ClockQuality = ClockQuality.Synced,
        readings: List<SensorReading> = listOf(reading("temperature", 24.0 + version)),
        battery: BatteryStatus? = BatteryStatus(11.4, null, null, "voltage_only"),
        id: FrameId = FrameId(UUID(0xF, version)),
    ) = TelemetryFrame(
        id = id,
        schemaVersion = "1.1",
        deviceId = DEVICE,
        bootId = bootId,
        sequence = sequence,
        capturedAt = capturedAt,
        monotonicMs = 1_000 * sequence,
        clockQuality = clockQuality,
        missionId = null,
        mapVersionId = null,
        pose = null,
        readings = readings,
        battery = battery,
        robotState = RobotReportedState("moving"),
        motorsActive = true,
        firmwareVersion = "0.2.0",
        receivedAt = receivedAt,
        streamVersion = StreamVersion(version),
    )
}
