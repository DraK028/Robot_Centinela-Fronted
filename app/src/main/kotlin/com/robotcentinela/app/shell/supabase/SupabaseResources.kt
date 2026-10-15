package com.robotcentinela.app.shell.supabase

/**
 * ÚNICO lugar con los nombres de recursos de Supabase que consume la app.
 *
 * PROVISIONAL: el equipo de backend todavía no publica el esquema definitivo. Los nombres de tablas y
 * columnas vienen de la propuesta de modelo del expediente técnico (§13.2). El recurso de frames sigue
 * la estructura de `JSON_estructura.txt`. Cuando backend publique el contrato, basta con actualizar
 * este archivo y los DTO de cada adaptador (ver docs/pendientes.md).
 */
object SupabaseResources {

    /** Esquema expuesto por PostgREST. */
    const val SCHEMA = "public"

    /**
     * PROVISIONAL. Vista, tabla o RPC que devuelve cada frame con la estructura de
     * `JSON_estructura.txt` más `robot_id`, `received_at` y `stream_version`.
     */
    const val TELEMETRY_FRAMES_VIEW = "telemetry_frames_v1"

    /**
     * Tabla que se observa con Realtime. Su nombre viene del expediente (§7.4: "Un cambio sobre
     * telemetry_frames se consulta con sus hijos después del commit"). Postgres Changes funciona sobre
     * tablas, no sobre vistas.
     */
    const val TELEMETRY_FRAMES_TABLE = "telemetry_frames"

    /** Membresías (§13.2). */
    const val MEMBERSHIPS = "memberships"

    /** Domicilios (§13.2). Se consultan embebidos en [MEMBERSHIPS]. */
    const val HOMES = "homes"

    /** Robots (§13.2). */
    const val ROBOTS = "robots"

    /** Alertas o incidentes (§13.2). La lógica que las genera vive en la base de datos. */
    const val ALERTS = "alerts"

    /** Columnas usadas en filtros y orden. */
    object Columns {
        const val ROBOT_ID = "robot_id"
        const val HOME_ID = "home_id"
        const val USER_ID = "user_id"
        const val STREAM_VERSION = "stream_version"
        const val CAPTURED_AT = "captured_at"
        const val MISSION_ID = "mission_id"
        const val OPENED_AT = "opened_at"
        const val TYPE = "type"
        const val SEVERITY = "severity"
        const val STATE = "state"
    }
}
