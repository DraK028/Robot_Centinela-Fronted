package com.robotcentinela.app.ui.model

import java.time.LocalDate

/** Rangos predefinidos del selector "Rango de datos" (propuesta de interfaz, HU07). */
enum class RangePreset(val label: String) {
    Today("Hoy"),
    Last7Days("Últimos 7 días"),
    ThisMonth("Este mes"),
    Last30Days("Últimos 30 días"),
    Custom("Personalizado"),
}

/**
 * Rango de días LOCALES, ambos inclusive. Los casos de uso lo convierten a UTC con la zona del
 * domicilio (HU07 CDA 1); la interfaz nunca calcula instantes UTC por su cuenta.
 */
data class DateRange(val from: LocalDate, val to: LocalDate, val preset: RangePreset) {

    init {
        require(!to.isBefore(from)) { "La fecha final no puede ser anterior a la inicial" }
    }

    val days: Long get() = to.toEpochDay() - from.toEpochDay() + 1

    val label: String
        get() = when (preset) {
            RangePreset.Custom -> if (from == to) Formats.date(from) else "${Formats.shortDate(from)} – ${Formats.shortDate(to)}"
            else -> preset.label
        }

    companion object {
        fun of(preset: RangePreset, today: LocalDate): DateRange = when (preset) {
            RangePreset.Today -> DateRange(today, today, preset)
            RangePreset.Last7Days -> DateRange(today.minusDays(6), today, preset)
            RangePreset.ThisMonth -> DateRange(today.withDayOfMonth(1), today, preset)
            RangePreset.Last30Days -> DateRange(today.minusDays(29), today, preset)
            RangePreset.Custom -> DateRange(today, today, preset)
        }

        fun custom(from: LocalDate, to: LocalDate) = DateRange(from, to, RangePreset.Custom)
    }
}
