package com.robotcentinela.app.ui.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

/**
 * Formatos de presentación en español de México (RNF13: fechas en la zona del domicilio).
 *
 * Los números usan punto decimal y no agrupan miles, como en la propuesta de interfaz ("1860 ADC").
 */
object Formats {

    val locale: Locale = Locale.forLanguageTag("es-MX")

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", locale)
    private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale)
    private val dateTimeSecondsFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", locale)

    fun number(value: Double, decimals: Int): String =
        String.format(locale, "%.${decimals.coerceIn(0, 6)}f", value)

    fun time(instant: Instant, zone: ZoneId): String = timeFormatter.format(instant.atZone(zone))

    fun shortTime(instant: Instant, zone: ZoneId): String = shortTimeFormatter.format(instant.atZone(zone))

    fun date(date: LocalDate): String = dateFormatter.format(date)

    fun shortDate(date: LocalDate): String = shortDateFormatter.format(date)

    fun dateTime(instant: Instant, zone: ZoneId): String = dateTimeFormatter.format(instant.atZone(zone))

    fun dateTimeSeconds(instant: Instant, zone: ZoneId): String = dateTimeSecondsFormatter.format(instant.atZone(zone))

    /** Fecha y hora si no es de hoy; solo la hora si es de hoy. */
    fun smartDateTime(instant: Instant, zone: ZoneId, today: LocalDate): String {
        val local = instant.atZone(zone)
        return if (local.toLocalDate() == today) time(instant, zone) else dateTime(instant, zone)
    }

    /** Edad legible: "hace 2 s", "hace 3 min", "hace 2 h", "hace 4 d". */
    fun age(duration: Duration): String {
        val seconds = duration.inWholeSeconds.coerceAtLeast(0)
        return when {
            seconds < 60 -> "hace $seconds s"
            seconds < 3_600 -> "hace ${seconds / 60} min"
            seconds < 86_400 -> "hace ${seconds / 3_600} h"
            else -> "hace ${seconds / 86_400} d"
        }
    }

    /** Duración legible: "45 s", "6 min 40 s", "1 h 05 min". */
    fun duration(duration: Duration): String {
        val seconds = duration.inWholeSeconds.coerceAtLeast(0)
        val h = seconds / 3_600
        val m = (seconds % 3_600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "$h h ${m.toString().padStart(2, '0')} min"
            m > 0 -> "$m min ${s.toString().padStart(2, '0')} s"
            else -> "$s s"
        }
    }
}
