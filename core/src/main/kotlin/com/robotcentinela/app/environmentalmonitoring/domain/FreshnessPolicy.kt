package com.robotcentinela.app.environmentalmonitoring.domain

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/** Edad de un dato y si se considera atrasado (HU04, RF09). */
sealed interface DataFreshness {

    /** Tiempo transcurrido desde la muestra. */
    val age: Duration

    /** El dato está dentro del tiempo esperado. */
    data class Fresh(override val age: Duration) : DataFreshness

    /** El dato excedió el límite sin actualizarse ("atrasado", HU04 CDA 1). */
    data class Stale(override val age: Duration, val limit: Duration) : DataFreshness
}

/**
 * Regla de frescura por canal (HU04 CDA 1, RF09, RNF04).
 *
 * Un canal pasa a "atrasado" cuando su última muestra supera [stalePeriods] periodos esperados,
 * aunque otros canales sigan activos. Los valores por defecto son los iniciales del expediente y
 * se deben ajustar con el jitter real (RNF04):
 * - [defaultExpectedPeriod] = 2 s: un frame cada 2 s por MQTT (§7, §7.4).
 * - [stalePeriods] = 3: "canal atrasado después de 3 periodos esperados" (RNF04).
 */
class FreshnessPolicy(
    val defaultExpectedPeriod: Duration = 2.seconds,
    val stalePeriods: Int = 3,
) {
    init {
        require(defaultExpectedPeriod.isPositive()) { "El periodo esperado debe ser positivo" }
        require(stalePeriods > 0) { "El número de periodos debe ser positivo" }
    }

    /**
     * @param sampleInstant instante de la muestra: `captured_at` confiable o, en su defecto,
     * `received_at` como aproximación explícita (§7.4).
     * @param expectedPeriod periodo del canal según el catálogo; si falta se usa
     * [defaultExpectedPeriod].
     * @param now instante actual del dispositivo.
     */
    fun evaluate(sampleInstant: Instant, expectedPeriod: Duration?, now: Instant): DataFreshness {
        // Si el reloj del teléfono va atrasado respecto a la muestra, la edad queda en cero en
        // lugar de ser negativa.
        val age = java.time.Duration.between(sampleInstant, now).toKotlinDuration()
            .coerceAtLeast(Duration.ZERO)
        val limit = (expectedPeriod?.takeIf { it.isPositive() } ?: defaultExpectedPeriod) * stalePeriods
        return if (age > limit) DataFreshness.Stale(age, limit) else DataFreshness.Fresh(age)
    }
}
