package com.robotcentinela.app.homemapping.domain

/**
 * Pose estimada del robot al capturar un frame (`pose` del JSON; `frame_poses`, expediente §13.2).
 *
 * Reglas:
 * - La pose es una **estimación**, no una medida (RNF12). Su incertidumbre se muestra
 *   distinguible de la huella del robot (HU06 CDA 3).
 * - Si un frame no trae pose, no hay posición: nunca se dibuja en el origen (RF12).
 * - Esta clase no asigna la lectura a una habitación. Eso requiere geometría de mapa validada y
 *   una posición confiable (RB11, HU06 CDA 1), y queda pendiente.
 *
 * @property xM posición X en metros sobre la versión de mapa del frame.
 * @property yM posición Y en metros.
 * @property thetaRad orientación en radianes.
 * @property sigmaPositionM incertidumbre de posición en metros. `null` = desconocida, nunca cero.
 * @property quality calidad de la pose tal como la reporta el dispositivo (p. ej. `estimated`).
 * El catálogo completo no está documentado, así que se conserva como texto.
 */
data class Pose(
    val xM: Double,
    val yM: Double,
    val thetaRad: Double,
    val sigmaPositionM: Double?,
    val quality: String,
) {
    init {
        require(xM.isFinite() && yM.isFinite() && thetaRad.isFinite()) { "La pose debe ser finita" }
        require(sigmaPositionM == null || (sigmaPositionM.isFinite() && sigmaPositionM >= 0)) {
            "La incertidumbre de posición no puede ser negativa"
        }
    }
}
