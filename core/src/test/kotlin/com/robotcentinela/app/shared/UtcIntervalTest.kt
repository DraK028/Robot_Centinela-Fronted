package com.robotcentinela.app.shared

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** HU07 CDA 1, RF10, RNF13. */
class UtcIntervalTest {

    @Test
    fun `un dia local en Ciudad de Mexico corresponde al intervalo UTC correcto`() {
        val day = LocalDate.of(2026, 10, 11)
        val interval = UtcInterval.fromLocalDates(day, day)
        assertEquals(Instant.parse("2026-10-11T06:00:00Z"), interval.start)
        assertEquals(Instant.parse("2026-10-12T06:00:00Z"), interval.endExclusive)
    }

    @Test
    fun `el fin es exclusivo`() {
        val day = LocalDate.of(2026, 10, 11)
        val interval = UtcInterval.fromLocalDates(day, day)
        assertTrue(Instant.parse("2026-10-12T05:59:59Z") in interval)
        assertFalse(Instant.parse("2026-10-12T06:00:00Z") in interval)
    }

    @Test
    fun `un dia con cambio de horario dura 23 horas`() {
        val day = LocalDate.of(2026, 3, 8)
        val interval = UtcInterval.fromLocalDates(day, day, ZoneId.of("America/New_York"))
        assertEquals(Duration.ofHours(23), Duration.between(interval.start, interval.endExclusive))
    }

    @Test
    fun `fecha final anterior a la inicial se rechaza`() {
        assertFailsWith<IllegalArgumentException> {
            UtcInterval.fromLocalDates(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 10))
        }
    }
}
