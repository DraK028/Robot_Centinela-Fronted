package com.robotcentinela.app.robothealth.domain

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * Estado de la ingesta de telemetría, visto desde la app (RNF04, RF29).
 *
 * Una ingesta interrumpida **no** significa que el robot esté apagado: el corte puede estar en el
 * puente, el broker o la red (RF29: "no se atribuye automáticamente a robot apagado").
 */
sealed interface IngestStatus {

    /** Todavía no se ha recibido ningún frame. */
    data object NoDataYet : IngestStatus

    /** Se están recibiendo frames dentro del límite. */
    data class Receiving(val lastReceivedAt: Instant, val sinceLast: Duration) : IngestStatus

    /** No llegan frames desde hace más del límite. */
    data class Interrupted(val lastReceivedAt: Instant, val sinceLast: Duration) : IngestStatus
}

/**
 * Regla de ingesta interrumpida (RNF04: "offline de ingesta inicialmente después de 10 s sin
 * received_at").
 *
 * Compara `received_at`, que asigna el servidor, con la hora del teléfono. Si ambos relojes difieren
 * mucho, el resultado se desplaza. Por eso el valor es inicial y se ajusta con el jitter real.
 */
class IngestConnectivityPolicy(val offlineAfter: Duration = 10.seconds) {

    init {
        require(offlineAfter.isPositive()) { "El límite de ingesta debe ser positivo" }
    }

    fun evaluate(lastReceivedAt: Instant?, now: Instant): IngestStatus {
        if (lastReceivedAt == null) return IngestStatus.NoDataYet
        val since = java.time.Duration.between(lastReceivedAt, now).toKotlinDuration()
            .coerceAtLeast(Duration.ZERO)
        return if (since > offlineAfter) {
            IngestStatus.Interrupted(lastReceivedAt, since)
        } else {
            IngestStatus.Receiving(lastReceivedAt, since)
        }
    }
}
