package com.robotcentinela.app.environmentalmonitoring.application

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelCapability
import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.DataFreshness
import com.robotcentinela.app.environmentalmonitoring.domain.FreshnessPolicy
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingInterpretation
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingQuality
import com.robotcentinela.app.testing.TestFrames.frame
import com.robotcentinela.app.testing.TestFrames.reading
import com.robotcentinela.app.telemetry.domain.LatestTelemetryProjection
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** HU02, HU04, HU20, RF05, RF09. */
class DescribeChannelStatusTest {

    private val latest = frame(
        version = 1,
        readings = listOf(
            reading("mq6_raw", 1860.0, ReadingQuality.Uncalibrated),
            reading("mq7_raw", null, ReadingQuality.NotInstalled),
        ),
    )
    private val projection = LatestTelemetryProjection().apply(latest)

    private fun describeAt(secondsAfterCapture: Long, capabilities: List<ChannelCapability> = emptyList()) =
        DescribeChannelStatus(
            FreshnessPolicy(),
            Clock.fixed(latest.capturedAt!!.plusSeconds(secondsAfterCapture), ZoneOffset.UTC),
        )(projection, capabilities)

    @Test
    fun `cada canal recibido se interpreta con su calidad y su edad`() {
        val statuses = describeAt(4).associateBy { it.channelKey.value }

        assertIs<ReadingInterpretation.RawSignal>(statuses.getValue("mq6_raw").interpretation)
        assertEquals(ReadingInterpretation.NotInstalled, statuses.getValue("mq7_raw").interpretation)
        assertIs<DataFreshness.Fresh>(statuses.getValue("mq6_raw").freshness)
    }

    @Test
    fun `un canal sin actualizacion pasa a atrasado`() {
        val status = describeAt(60).first { it.channelKey.value == "mq6_raw" }
        assertIs<DataFreshness.Stale>(status.freshness)
    }

    @Test
    fun `un canal del catalogo que nunca reporto es sin dato`() {
        val catalogOnly = ChannelCapability(ChannelKey("chlorine"), "gas", null, "Cl2", null, enabled = true)

        val status = describeAt(4, listOf(catalogOnly)).first { it.channelKey.value == "chlorine" }

        assertEquals(ReadingInterpretation.NoData, status.interpretation)
        assertNull(status.freshness)
        assertNull(status.sample)
    }
}
