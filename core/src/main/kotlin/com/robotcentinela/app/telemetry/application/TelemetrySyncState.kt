package com.robotcentinela.app.telemetry.application

import com.robotcentinela.app.telemetry.domain.LatestTelemetryProjection
import com.robotcentinela.app.telemetry.domain.StreamVersion
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Estado de la sincronización, según la condición real (§7.4: "Mostrar conectando, en vivo, datos
 * atrasados, sin conexión o error según condición real").
 */
sealed interface SyncStatus {

    /** La sincronización todavía no se ha iniciado. */
    data object Idle : SyncStatus

    /** Primera conexión: se espera la suscripción y el snapshot inicial. */
    data object Connecting : SyncStatus

    /**
     * Se recuperan los deltas tras una reconexión o un regreso desde segundo plano, hasta alcanzar
     * el watermark (RB28).
     */
    data object Reconciling : SyncStatus

    /** Suscrito y al día con los frames confirmados. */
    data object Live : SyncStatus

    /** La suscripción se interrumpió. Se muestran los últimos datos aplicados, con su hora. */
    data class Offline(val reason: String) : SyncStatus

    /** Falló una consulta. Se reintenta con el siguiente aviso o en la reconciliación periódica. */
    data class Error(val message: String) : SyncStatus

    /**
     * Sincronización detenida, por ejemplo con la app en segundo plano. Nunca se describe como en
     * vivo (RB28).
     */
    data object Stopped : SyncStatus
}

/**
 * Estado observable de la sincronización de un robot.
 *
 * @property projection último dato por canal y por frame.
 * @property lastAppliedVersion último `stream_version` aplicado (el `last_applied_version` de §7.4).
 * @property watermark watermark del último snapshot aplicado.
 * @property rejectedFrames frames persistidos que no cumplen el contrato de la app (RF29).
 * @property historyLimited `true` si el cursor quedó fuera de la retención y se aplicó un snapshot
 * completo. El historial intermedio ya no está disponible (RB26, T08).
 */
data class TelemetrySyncState(
    val status: SyncStatus = SyncStatus.Idle,
    val projection: LatestTelemetryProjection = LatestTelemetryProjection(),
    val lastAppliedVersion: StreamVersion? = null,
    val watermark: StreamVersion? = null,
    val rejectedFrames: Long = 0,
    val historyLimited: Boolean = false,
)

/**
 * Parámetros de la sincronización. Los valores por defecto son iniciales y se deben ajustar con
 * pruebas (ADR-004).
 *
 * @property deltaPageSize frames por página al pedir deltas. La documentación no fija este valor;
 * es un parámetro provisional.
 * @property reconciliationInterval reconciliación periódica para cubrir avisos omitidos (§7.4:
 * "cada 30 s como parámetro inicial").
 * @property dedupeWindow cuántos `frame_id` recientes se recuerdan para deduplicar. Equivale a los
 * frames de una misión de 90 min a un frame cada 2 s (§7.5: 2,700 frames).
 */
data class SyncConfig(
    val deltaPageSize: Int = 100,
    val reconciliationInterval: Duration = 30.seconds,
    val dedupeWindow: Int = 2_700,
) {
    init {
        require(deltaPageSize > 0) { "deltaPageSize debe ser positivo" }
        require(reconciliationInterval.isPositive()) { "reconciliationInterval debe ser positivo" }
        require(dedupeWindow > 0) { "dedupeWindow debe ser positivo" }
    }
}
