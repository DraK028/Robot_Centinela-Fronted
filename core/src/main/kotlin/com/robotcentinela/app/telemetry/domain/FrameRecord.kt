package com.robotcentinela.app.telemetry.domain

/**
 * Registro del flujo de un robot, tal como lo entrega el puerto de consulta.
 *
 * Cada `stream_version` produce exactamente un registro. Así el cursor avanza aunque un frame no se
 * pueda interpretar, y esos frames se cuentan en lugar de ocultarse (RF29).
 */
sealed interface FrameRecord {
    val streamVersion: StreamVersion

    /** Frame que cumple el contrato y se interpretó correctamente. */
    data class Interpreted(val frame: TelemetryFrame) : FrameRecord {
        override val streamVersion: StreamVersion get() = frame.streamVersion
    }

    /**
     * Frame persistido que no cumple el contrato que espera la app (por ejemplo, una
     * `schema_version` no soportada).
     *
     * @property reason causa legible, sin incluir el payload (RNF19).
     */
    data class Rejected(
        override val streamVersion: StreamVersion,
        val frameId: FrameId?,
        val reason: String,
    ) : FrameRecord
}
