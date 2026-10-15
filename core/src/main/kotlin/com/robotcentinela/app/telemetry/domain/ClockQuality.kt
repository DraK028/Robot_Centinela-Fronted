package com.robotcentinela.app.telemetry.domain

/**
 * Calidad del reloj del dispositivo al capturar (`clock_quality`, §7.2).
 *
 * Con un reloj no sincronizado, `captured_at` no se muestra como exacto. Se ordena con
 * `received_at` como aproximación explícita (§7.4, RNF13).
 */
sealed interface ClockQuality {
    val raw: String

    data object Synced : ClockQuality {
        override val raw = "synced"
    }

    data object Unsynced : ClockQuality {
        override val raw = "unsynced"
    }

    /** Valor no documentado. Se trata como reloj no confiable. */
    data class Unknown(override val raw: String) : ClockQuality

    companion object {
        fun fromRaw(raw: String): ClockQuality = when (raw) {
            Synced.raw -> Synced
            Unsynced.raw -> Unsynced
            else -> Unknown(raw)
        }
    }
}
