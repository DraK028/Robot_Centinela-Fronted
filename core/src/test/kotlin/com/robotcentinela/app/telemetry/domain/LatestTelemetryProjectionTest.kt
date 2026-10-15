package com.robotcentinela.app.telemetry.domain

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.testing.TestFrames
import com.robotcentinela.app.testing.TestFrames.frame
import com.robotcentinela.app.testing.TestFrames.reading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RB19, RF09 y caso T06 (reloj y orden). */
class LatestTelemetryProjectionTest {

    private val temperature = ChannelKey("temperature")

    @Test
    fun `una muestra atrasada no reemplaza un dato mas reciente`() {
        val recent = frame(version = 10, sequence = 50)
        // Frame capturado antes, pero confirmado después (por ejemplo, tras una desconexión).
        val late = frame(version = 11, sequence = 40)

        val projection = LatestTelemetryProjection().apply(recent).apply(late)

        assertEquals(recent.id, projection.channels.getValue(temperature).stamp.frameId)
        assertEquals(recent, projection.latestFrame)
    }

    @Test
    fun `received_at mas reciente se conserva aunque el frame sea atrasado`() {
        val recent = frame(version = 10, sequence = 50)
        val late = frame(version = 11, sequence = 40, receivedAt = recent.receivedAt.plusSeconds(60))

        val projection = LatestTelemetryProjection().apply(recent).apply(late)

        assertEquals(late.receivedAt, projection.lastReceivedAt)
    }

    @Test
    fun `entre boots distintos con reloj sincronizado manda captured_at`() {
        val before = frame(version = 1, sequence = 900, bootId = TestFrames.BOOT_1)
        val afterReboot = frame(version = 2, sequence = 1, bootId = TestFrames.BOOT_2)

        val projection = LatestTelemetryProjection().apply(before).apply(afterReboot)

        assertEquals(afterReboot.id, projection.channels.getValue(temperature).stamp.frameId)
        assertFalse(projection.channels.getValue(temperature).stamp.timeIsApproximate)
    }

    @Test
    fun `sin reloj sincronizado se usa received_at como aproximacion explicita`() {
        val first = frame(version = 1, bootId = TestFrames.BOOT_1)
        val unsynced = frame(
            version = 2,
            bootId = TestFrames.BOOT_2,
            capturedAt = TestFrames.T0.minusSeconds(3_600),
            clockQuality = ClockQuality.Unsynced,
        )

        val projection = LatestTelemetryProjection().apply(first).apply(unsynced)

        val stamp = projection.channels.getValue(temperature).stamp
        assertEquals(unsynced.id, stamp.frameId)
        assertTrue(stamp.timeIsApproximate)
        assertEquals(unsynced.receivedAt, stamp.referenceInstant)
    }

    @Test
    fun `cada canal conserva su propia muestra`() {
        val both = frame(version = 1, readings = listOf(reading("temperature", 24.0), reading("humidity", 48.0)))
        val onlyTemperature = frame(version = 2, readings = listOf(reading("temperature", 25.0)))

        val projection = LatestTelemetryProjection().apply(both).apply(onlyTemperature)

        assertEquals(onlyTemperature.id, projection.channels.getValue(temperature).stamp.frameId)
        assertEquals(both.id, projection.channels.getValue(ChannelKey("humidity")).stamp.frameId)
    }
}
