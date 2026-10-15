package com.robotcentinela.app.environmentalmonitoring.domain

/**
 * Qué se puede afirmar de una lectura, según su calidad y su valor.
 *
 * Es la regla común para web y móvil (RB20). Impide los errores semánticos que prohíbe RNF12:
 * confundir ADC con ppm, un gas ausente con cero o un silencio con aire seguro.
 */
sealed interface ReadingInterpretation {

    /**
     * Lectura válida o estimada con valor.
     *
     * @property unitCode unidad del catálogo de capacidades; `null` si el catálogo no está disponible.
     */
    data class Measured(
        val value: ReadingValue,
        val unitCode: String?,
        val quality: ReadingQuality,
    ) : ReadingInterpretation

    /**
     * Señal cruda sin calibrar, como el ADC de un MQ (HU02 CDA 1).
     *
     * Nunca lleva una unidad de concentración (RB05).
     */
    data class RawSignal(val value: ReadingValue, val unitCode: String?) : ReadingInterpretation

    /** Sensor no instalado: estado propio, nunca cero (HU02 CDA 2, RF05). */
    data object NotInstalled : ReadingInterpretation

    /** Sensor calentando: no se muestra concentración (HU20 CDA 1, §12.5). */
    data object Warming : ReadingInterpretation

    /** Sensor con fallo (HU20 CDA 1). */
    data object Fault : ReadingInterpretation

    /** Lectura fuera de rango o saturada (HU20 CDA 1). */
    data object OutOfRange : ReadingInterpretation

    /** No hay valor ("Sin dato"). Nunca se representa como cero ni como aire seguro (RB04, RB09). */
    data object NoData : ReadingInterpretation

    /**
     * La calidad no está en el catálogo. El valor se conserva, pero no se afirma su validez.
     */
    data class Uncertain(val value: ReadingValue, val rawQuality: String) : ReadingInterpretation
}

/**
 * Regla de interpretación de lecturas (HU02, HU20, RB04, RB05, RB09).
 *
 * Orden de precedencia:
 * 1. Los estados del sensor (`not_installed`, `warming`, `fault`, `out_of_range`) mandan sobre
 *    cualquier valor.
 * 2. Si no hay valor, el resultado es [ReadingInterpretation.NoData].
 * 3. `uncalibrated` produce una señal cruda sin unidad de concentración.
 * 4. `valid` y `estimated` producen una medida con la unidad del catálogo.
 * 5. Una calidad desconocida produce [ReadingInterpretation.Uncertain].
 */
object ReadingInterpreter {

    /**
     * Unidades de concentración que una señal sin calibrar nunca puede llevar (RB05). `ppm` es la
     * única unidad de concentración que menciona el contrato (§7.2: "unidades registradas como ppm,
     * porcentaje o adc_count").
     */
    val CONCENTRATION_UNITS: Set<String> = setOf("ppm")

    fun interpret(reading: SensorReading, capability: ChannelCapability? = null): ReadingInterpretation {
        val quality = reading.quality
        when (quality) {
            ReadingQuality.NotInstalled -> return ReadingInterpretation.NotInstalled
            ReadingQuality.Warming -> return ReadingInterpretation.Warming
            ReadingQuality.Fault -> return ReadingInterpretation.Fault
            ReadingQuality.OutOfRange -> return ReadingInterpretation.OutOfRange
            else -> Unit
        }

        if (reading.value == ReadingValue.Absent) return ReadingInterpretation.NoData

        val unitCode = capability?.unitCode
        return when (quality) {
            ReadingQuality.Uncalibrated -> ReadingInterpretation.RawSignal(
                value = reading.value,
                unitCode = unitCode?.takeUnless { it in CONCENTRATION_UNITS },
            )
            ReadingQuality.Valid, ReadingQuality.Estimated ->
                ReadingInterpretation.Measured(reading.value, unitCode, quality)
            else -> ReadingInterpretation.Uncertain(reading.value, quality.raw)
        }
    }
}
