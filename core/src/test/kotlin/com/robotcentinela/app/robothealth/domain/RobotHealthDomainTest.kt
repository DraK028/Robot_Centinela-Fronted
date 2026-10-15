package com.robotcentinela.app.robothealth.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

/** HU08, RF18, RB17 (batería) y RNF04, RF29 (ingesta). */
class RobotHealthDomainTest {

    @Test
    fun `solo voltaje muestra porcentaje no disponible`() {
        val result = BatteryInterpretation.of(BatteryStatus(11.4, null, null, "voltage_only"))
        assertEquals(BatteryInterpretation.PercentageUnavailable(11.4, "voltage_only"), result)
    }

    @Test
    fun `con SOC estimado se muestra porcentaje y metodo`() {
        val result = BatteryInterpretation.of(BatteryStatus(11.4, 80.0, 0.5, "estimated"))
        assertEquals(BatteryInterpretation.WithPercentage(11.4, 80.0, "estimated"), result)
    }

    @Test
    fun `SOC fuera de 0 a 100 no se muestra`() {
        val result = BatteryInterpretation.of(BatteryStatus(11.4, 120.0, null, "estimated"))
        assertIs<BatteryInterpretation.PercentageUnavailable>(result)
    }

    @Test
    fun `sin bateria no hay dato`() {
        assertEquals(BatteryInterpretation.NoData, BatteryInterpretation.of(null))
        assertEquals(BatteryInterpretation.NoData, BatteryInterpretation.of(BatteryStatus(null, null, null, "x")))
    }

    private val policy = IngestConnectivityPolicy()
    private val received = Instant.parse("2026-10-11T15:24:12Z")

    @Test
    fun `sin frames la ingesta no tiene datos todavia`() {
        assertEquals(IngestStatus.NoDataYet, policy.evaluate(null, received))
    }

    @Test
    fun `hasta 10 s sin received_at la ingesta sigue recibiendo`() {
        assertEquals(IngestStatus.Receiving(received, 10.seconds), policy.evaluate(received, received.plusSeconds(10)))
    }

    @Test
    fun `mas de 10 s sin received_at es ingesta interrumpida`() {
        assertEquals(IngestStatus.Interrupted(received, 11.seconds), policy.evaluate(received, received.plusSeconds(11)))
    }
}
