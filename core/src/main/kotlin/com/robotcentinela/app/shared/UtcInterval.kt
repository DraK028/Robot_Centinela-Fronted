package com.robotcentinela.app.shared

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Intervalo temporal semiabierto `[start, endExclusive)` expresado en UTC.
 *
 * Toda consulta de historial viaja al backend en UTC (RNF13: `timestamptz` en base). El usuario,
 * en cambio, filtra por fechas locales. Esta clase hace esa conversión en un solo lugar
 * (HU07 CDA 1, RF10: "Filtro de un día en zona local corresponde al intervalo UTC correcto").
 */
data class UtcInterval(val start: Instant, val endExclusive: Instant) {

    init {
        require(start < endExclusive) { "El inicio del intervalo debe ser anterior a su fin" }
    }

    /** Indica si [instant] cae dentro del intervalo. */
    operator fun contains(instant: Instant): Boolean = instant >= start && instant < endExclusive

    companion object {

        /**
         * Zona horaria de presentación por defecto (RNF13: "Las fechas se presentan en
         * America/Mexico_City por defecto"). Coincide con el valor por defecto de `homes.timezone`
         * del ejemplo SQL del expediente (§13.6).
         */
        val DEFAULT_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

        /**
         * Convierte un rango de días locales, ambos inclusive, al intervalo UTC equivalente.
         *
         * El inicio es la medianoche local de [from] y el fin es la medianoche local del día
         * siguiente a [toInclusive]. `atStartOfDay(zone)` resuelve correctamente los días con
         * cambio de horario.
         *
         * @param zone zona del domicilio (`homes.timezone`); por defecto [DEFAULT_ZONE].
         */
        fun fromLocalDates(
            from: LocalDate,
            toInclusive: LocalDate,
            zone: ZoneId = DEFAULT_ZONE,
        ): UtcInterval {
            require(!toInclusive.isBefore(from)) { "La fecha final no puede ser anterior a la inicial" }
            return UtcInterval(
                start = from.atStartOfDay(zone).toInstant(),
                endExclusive = toInclusive.plusDays(1).atStartOfDay(zone).toInstant(),
            )
        }
    }
}
