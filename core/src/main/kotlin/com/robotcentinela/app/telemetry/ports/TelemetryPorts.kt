package com.robotcentinela.app.telemetry.ports

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.telemetry.domain.FrameRecord
import com.robotcentinela.app.telemetry.domain.StreamVersion
import kotlinx.coroutines.flow.Flow

/**
 * Resultado de un snapshot (§7.4: "snapshot consistente con watermark").
 *
 * @property records último estado conocido del robot.
 * @property watermark versión de flujo hasta la que el snapshot declara consistencia (glosario
 * §19.2); `null` si el robot todavía no tiene frames.
 */
data class TelemetrySnapshot(val records: List<FrameRecord>, val watermark: StreamVersion?)

/**
 * Puerto de consulta de frames persistidos.
 *
 * Corresponde a los contratos lógicos del expediente (§7.3): `GET /v1/robots/{id}/snapshot` y
 * `GET /v1/robots/{id}/frames?after_version=n`. Su implementación puede ser una consulta, una RPC o
 * una Edge Function.
 */
interface TelemetryQueryPort {

    /** Último estado del robot con su watermark. */
    suspend fun snapshot(robotId: RobotId): TelemetrySnapshot

    /**
     * Frames con `stream_version` estrictamente mayor que [after], en orden ascendente, como máximo
     * [limit] (RF08: deltas persistidos).
     */
    suspend fun framesAfter(robotId: RobotId, after: StreamVersion, limit: Int): List<FrameRecord>

    /**
     * Historial por intervalo de captura en UTC, opcionalmente de una misión, paginado (RF10).
     * Orden ascendente por captura.
     */
    suspend fun framesBetween(
        robotId: RobotId,
        interval: UtcInterval,
        missionId: MissionId?,
        page: PageRequest,
    ): List<FrameRecord>
}

/** Aviso de cambio en el flujo de un robot (Realtime). */
sealed interface TelemetryNotice {

    /** La suscripción autorizada quedó confirmada (§7.4: "esperar confirmación"). */
    data object Subscribed : TelemetryNotice

    /** Se confirmó al menos un frame nuevo. El aviso no trae datos: se consultan los deltas. */
    data object FrameCommitted : TelemetryNotice

    /** La suscripción se interrumpió. Al confirmarse otra vez, se reconcilia. */
    data class Interrupted(val reason: String) : TelemetryNotice
}

/**
 * Puerto de avisos en vivo.
 *
 * Realtime **no** es una cola durable ni un historial. Solo avisa que hay cambios confirmados; los
 * datos se obtienen siempre del [TelemetryQueryPort] (§7.3, RB26).
 */
interface TelemetryChangeNotifier {
    fun notices(robotId: RobotId): Flow<TelemetryNotice>
}
