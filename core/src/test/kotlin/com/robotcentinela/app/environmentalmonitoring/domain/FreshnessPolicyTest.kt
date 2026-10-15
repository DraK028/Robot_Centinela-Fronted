package com.robotcentinela.app.environmentalmonitoring.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** HU04 CDA 1, RF09, RNF04. */
class FreshnessPolicyTest {

    private val policy = FreshnessPolicy()
    private val sample = Instant.parse("2026-10-11T15:24:12Z")

    @Test
    fun `dentro de tres periodos de 2 s el dato es actual`() {
        val result = policy.evaluate(sample, null, sample.plusSeconds(6))
        assertEquals(DataFreshness.Fresh(6.seconds), result)
    }

    @Test
    fun `al exceder tres periodos el canal pasa a atrasado`() {
        val result = assertIs<DataFreshness.Stale>(policy.evaluate(sample, null, sample.plusSeconds(7)))
        assertEquals(6.seconds, result.limit)
        assertEquals(7.seconds, result.age)
    }

    @Test
    fun `el periodo del catalogo cambia el limite`() {
        val result = policy.evaluate(sample, 10.seconds, sample.plusSeconds(25))
        assertIs<DataFreshness.Fresh>(result)
    }

    @Test
    fun `un reloj del telefono atrasado no produce edad negativa`() {
        val result = policy.evaluate(sample, null, sample.minusSeconds(5))
        assertEquals(Duration.ZERO, result.age)
    }
}
