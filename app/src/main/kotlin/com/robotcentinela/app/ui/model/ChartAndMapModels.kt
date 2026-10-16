package com.robotcentinela.app.ui.model

/**
 * Punto de la trayectoria (pose estimada de un frame, nunca medida, RNF12).
 *
 * @property sampling el robot estaba detenido muestreando en una estación. Las lecturas en tránsito
 * se ocultan por defecto en la vista espacial (§12.5).
 * @property signal señal relativa del canal elegido (p. ej. ADC del MQ6), si existe.
 */
data class TrajectoryPoint(
    val xM: Double,
    val yM: Double,
    val sigmaM: Double?,
    val sampling: Boolean,
    val signal: Double?,
)

/** Posición del robot con su incertidumbre. `sigmaM` nulo = desconocida, nunca cero (HU06). */
data class RobotMarker(val xM: Double, val yM: Double, val thetaRad: Double, val sigmaM: Double?)

/** Capas visibles del mapa. Las lecturas en tránsito están ocultas por defecto (§12.5). */
data class MapLayers(val route: Boolean = true, val samples: Boolean = true, val transit: Boolean = false)

/**
 * Punto de una serie temporal. `y` nulo = hueco real (sin muestras válidas): se deja vacío y nunca se
 * une con trazo continuo (§12.5, RF10).
 *
 * @property low mínimo del intervalo (banda), si existe.
 * @property high máximo del intervalo (banda), si existe.
 */
data class ChartPoint(val xMs: Long, val y: Double?, val low: Double? = null, val high: Double? = null)
