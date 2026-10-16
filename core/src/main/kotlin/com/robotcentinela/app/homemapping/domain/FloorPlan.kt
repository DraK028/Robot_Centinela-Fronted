package com.robotcentinela.app.homemapping.domain

/**
 * Geometría de una versión de mapa: el plano sobre el que se dibujan la ruta, la pose y las
 * estaciones (HU05, HU06, RF11, §12.5).
 *
 * **PROVISIONAL.** El formato de `map_versions` (habitaciones, estaciones, rutas) está pendiente de
 * contrato (docs/pendientes.md, "Mapas"). Este modelo solo contiene lo que la guía de interfaces pide
 * dibujar y se ajustará cuando backend publique el formato. Las coordenadas están en metros, en el
 * mismo marco que [Pose].
 *
 * @property semantic `true` si el plano es semántico (habitaciones con nombre validadas). La interfaz
 * lo rotula como tal (§12.5).
 * @property unexplored zonas sin explorar; se dibujan con trama neutra (§12.5).
 */
data class FloorPlan(
    val mapVersionId: MapVersionId,
    val widthM: Double,
    val heightM: Double,
    val semantic: Boolean,
    val rooms: List<Room>,
    val walls: List<WallSegment>,
    val stations: List<Station>,
    val unexplored: List<PlanRect> = emptyList(),
) {
    init {
        require(widthM.isFinite() && widthM > 0 && heightM.isFinite() && heightM > 0) {
            "Las dimensiones del plano deben ser positivas"
        }
    }

    /** Busca una estación por su identificador. */
    fun station(id: StationId): Station? = stations.firstOrNull { it.id == id }
}

/** Punto del plano en metros. */
data class PlanPoint(val xM: Double, val yM: Double)

/** Rectángulo del plano en metros (origen abajo a la izquierda, Y hacia arriba). */
data class PlanRect(val left: Double, val bottom: Double, val right: Double, val top: Double) {
    init {
        require(left < right && bottom < top) { "El rectángulo debe tener área positiva" }
    }

    val center: PlanPoint get() = PlanPoint((left + right) / 2, (bottom + top) / 2)
}

/** Habitación con nombre. Solo existe en planos validados; la app no asigna lecturas a habitaciones (RB11). */
data class Room(val name: String, val bounds: PlanRect)

/** Segmento de muro. */
data class WallSegment(val from: PlanPoint, val to: PlanPoint)

/** Identificador de estación de muestreo. */
@JvmInline
value class StationId(val value: String)

/** Estación de muestreo del recorrido (RF13, RF33). */
data class Station(val id: StationId, val name: String, val position: PlanPoint)
