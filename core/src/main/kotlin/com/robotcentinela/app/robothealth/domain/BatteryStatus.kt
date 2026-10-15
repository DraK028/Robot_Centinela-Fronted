package com.robotcentinela.app.robothealth.domain

/**
 * Batería reportada en un frame (`battery` del JSON; `battery_readings`, §13.2).
 *
 * @property voltageV tensión en voltios.
 * @property socPct estado de carga en porcentaje; `null` cuando no se estima.
 * @property currentA corriente en amperios; `null` cuando no se mide.
 * @property quality método o calidad declarada por el dispositivo (p. ej. `voltage_only`). Indica
 * cómo se obtuvo el dato (HU08 CDA 2). El catálogo completo no está documentado.
 */
data class BatteryStatus(
    val voltageV: Double?,
    val socPct: Double?,
    val currentA: Double?,
    val quality: String,
)

/** Qué se puede afirmar de la batería (HU08, RF18, RB17). */
sealed interface BatteryInterpretation {

    /** Hay porcentaje estimado por el método declarado. */
    data class WithPercentage(
        val voltageV: Double?,
        val socPct: Double,
        val method: String,
    ) : BatteryInterpretation

    /**
     * Solo hay tensión, o el porcentaje no es válido: se muestra "Porcentaje no disponible"
     * (HU08 CDA 1). La tensión nunca se convierte linealmente en porcentaje (RF18, RB17).
     */
    data class PercentageUnavailable(val voltageV: Double?, val method: String) : BatteryInterpretation

    /** El frame no trae información de batería. */
    data object NoData : BatteryInterpretation

    companion object {

        /**
         * Interpreta la batería de un frame.
         *
         * Un SOC fuera de 0 a 100 o una tensión negativa no se muestran: la base los restringe así
         * (§13.2: "SOC 0 a 100 o null; voltaje no negativo").
         */
        fun of(status: BatteryStatus?): BatteryInterpretation {
            if (status == null) return NoData
            val voltage = status.voltageV?.takeIf { it.isFinite() && it >= 0 }
            val soc = status.socPct?.takeIf { it.isFinite() && it in 0.0..100.0 }
            return when {
                soc != null -> WithPercentage(voltage, soc, status.quality)
                voltage != null -> PercentageUnavailable(voltage, status.quality)
                else -> NoData
            }
        }
    }
}
