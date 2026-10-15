package com.robotcentinela.app.telemetry.domain

import java.util.UUID

/**
 * Identidad inmutable de un frame (`event_id`; `telemetry_frames.id`, §13.2).
 *
 * Web y móvil usan el mismo identificador para un mismo evento (RF06, HU03).
 */
@JvmInline
value class FrameId(val value: UUID)

/** Dispositivo que capturó el frame (`device_id`; `devices`, §13.2). */
@JvmInline
value class DeviceId(val value: UUID)

/** Sesión de arranque del dispositivo (`boot_id`; `device_sessions`, §13.2). Cambia en cada reinicio. */
@JvmInline
value class BootId(val value: UUID)

/**
 * Cursor de commit por robot (`stream_version`, §7.4).
 *
 * El backend lo asigna dentro de la misma transacción que persiste el frame y lo incrementa en uno
 * por cada frame nuevo aceptado. Ordena la sincronización entre clientes (HU03, RB27). No sustituye
 * a `sequence`, que ordena la captura dentro de un boot.
 */
@JvmInline
value class StreamVersion(val value: Long) : Comparable<StreamVersion> {

    init {
        require(value >= 0) { "stream_version no puede ser negativo" }
    }

    override fun compareTo(other: StreamVersion): Int = value.compareTo(other.value)

    /** Versión inmediatamente siguiente. */
    fun next(): StreamVersion = StreamVersion(value + 1)

    companion object {
        /**
         * Valor antes del primer frame: el contador de `robots.last_stream_version` empieza en 0
         * (§13.6).
         */
        val NONE = StreamVersion(0)
    }
}
