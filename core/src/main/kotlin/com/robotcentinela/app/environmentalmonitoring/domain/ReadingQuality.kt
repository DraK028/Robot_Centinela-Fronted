package com.robotcentinela.app.environmentalmonitoring.domain

/**
 * Calidad de una lectura (`readings[].quality`).
 *
 * El catálogo viene del glosario del expediente (§19.2): "valid, estimated, uncalibrated, warming,
 * fault, out_of_range o not_installed". Un valor fuera del catálogo se conserva en [Unknown] y se
 * trata como calidad incierta. No se descarta ni se "corrige".
 */
sealed interface ReadingQuality {

    /** Texto original recibido. */
    val raw: String

    /** Lectura válida según el método del canal. */
    data object Valid : ReadingQuality {
        override val raw = "valid"
    }

    /** Lectura estimada (por ejemplo, con calibración declarada, §7.2). */
    data object Estimated : ReadingQuality {
        override val raw = "estimated"
    }

    /** Señal cruda sin calibración: ADC o voltaje, nunca ppm (RB05). */
    data object Uncalibrated : ReadingQuality {
        override val raw = "uncalibrated"
    }

    /** Sensor en calentamiento: no se muestra concentración (§12.5). */
    data object Warming : ReadingQuality {
        override val raw = "warming"
    }

    /** Sensor con fallo. */
    data object Fault : ReadingQuality {
        override val raw = "fault"
    }

    /** Lectura fuera del rango del sensor o saturada. */
    data object OutOfRange : ReadingQuality {
        override val raw = "out_of_range"
    }

    /** Sensor no instalado (HU02 CDA 2: "No instalado"). */
    data object NotInstalled : ReadingQuality {
        override val raw = "not_installed"
    }

    /** Valor no documentado en el catálogo. */
    data class Unknown(override val raw: String) : ReadingQuality

    companion object {
        private val catalog: List<ReadingQuality> by lazy {
            listOf(Valid, Estimated, Uncalibrated, Warming, Fault, OutOfRange, NotInstalled)
        }

        /** Convierte el texto recibido en el valor del catálogo, o en [Unknown]. */
        fun fromRaw(raw: String): ReadingQuality = catalog.firstOrNull { it.raw == raw } ?: Unknown(raw)
    }
}
