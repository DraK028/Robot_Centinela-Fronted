package com.robotcentinela.app.shell.demo

import com.robotcentinela.app.homemapping.domain.FloorPlan
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.homemapping.domain.PlanPoint
import com.robotcentinela.app.homemapping.domain.PlanRect
import com.robotcentinela.app.homemapping.domain.Room
import com.robotcentinela.app.homemapping.domain.Station
import com.robotcentinela.app.homemapping.domain.StationId
import com.robotcentinela.app.homemapping.domain.WallSegment
import java.util.UUID
import kotlin.math.hypot

/**
 * Planos y rutas SINTÉTICOS del simulador (HU15). No representan ninguna vivienda real.
 *
 * Hay tres versiones de mapa para probar HU05 y HU19:
 * - [MAP_V2]: plano vigente (semántico).
 * - [MAP_V1]: plano anterior, con la puerta de la cocina en otra posición. Las misiones antiguas se
 *   dibujan sobre él, no sobre el vigente (RB13).
 * - [MAP_LEGACY]: versión sin archivo de plano: la interfaz muestra la lista de estaciones (HU05 CDA 3).
 */
internal object DemoPlans {

    val MAP_V2 = MapVersionId(UUID.fromString("5a7e0000-0000-4000-8000-00000000a002"))
    val MAP_V1 = MapVersionId(UUID.fromString("5a7e0000-0000-4000-8000-00000000a001"))
    val MAP_LEGACY = MapVersionId(UUID.fromString("5a7e0000-0000-4000-8000-00000000a000"))

    /** Eje de circulación (y = 2.2 m) y posición de la base de carga. */
    private const val SPINE_Y = 2.2
    val DOCK = PlanPoint(3.6, SPINE_Y)

    /** Estación con el camino de acceso desde el eje (el último punto es la estación). */
    class StationSpec(val station: Station, val approach: List<PlanPoint>)

    /** Estaciones en el orden del recorrido. */
    fun stations(version: MapVersionId): List<StationSpec> {
        val kitchenDoorX = if (version == MAP_V2) 6.4 else 7.4
        return listOf(
            StationSpec(
                Station(StationId("E1"), "Recámara", PlanPoint(2.0, 1.4)),
                listOf(PlanPoint(2.0, SPINE_Y), PlanPoint(2.0, 1.4)),
            ),
            StationSpec(
                Station(StationId("E2"), "Sala", PlanPoint(2.0, 5.2)),
                listOf(PlanPoint(2.9, SPINE_Y), PlanPoint(2.9, 3.6), PlanPoint(2.0, 5.2)),
            ),
            StationSpec(
                Station(StationId("E3"), "Pasillo", PlanPoint(5.8, 2.25)),
                listOf(PlanPoint(5.8, SPINE_Y), PlanPoint(5.8, 2.25)),
            ),
            StationSpec(
                Station(StationId("E4"), "Cocina", PlanPoint(7.6, 5.2)),
                listOf(PlanPoint(kitchenDoorX, SPINE_Y), PlanPoint(kitchenDoorX, 3.6), PlanPoint(7.6, 5.2)),
            ),
            StationSpec(
                Station(StationId("E5"), "Baño", PlanPoint(8.7, 0.8)),
                listOf(PlanPoint(8.7, SPINE_Y), PlanPoint(8.7, 1.5), PlanPoint(8.7, 0.8)),
            ),
        )
    }

    /** Plano de una versión, o `null` si la versión no tiene archivo de plano. */
    fun floorPlan(version: MapVersionId): FloorPlan? {
        if (version != MAP_V2 && version != MAP_V1) return null
        val kitchenDoor = if (version == MAP_V2) 6.0 to 6.8 else 7.0 to 7.8
        val walls = mutableListOf<WallSegment>()

        fun wall(x1: Double, y1: Double, x2: Double, y2: Double) {
            walls += WallSegment(PlanPoint(x1, y1), PlanPoint(x2, y2))
        }

        // Perímetro.
        wall(0.0, 0.0, 10.0, 0.0)
        wall(10.0, 0.0, 10.0, 7.0)
        wall(10.0, 7.0, 0.0, 7.0)
        wall(0.0, 7.0, 0.0, 0.0)
        // Recámara | Sala (y = 3, puerta 2.5 a 3.3).
        wall(0.0, 3.0, 2.5, 3.0)
        wall(3.3, 3.0, 4.5, 3.0)
        // Recámara | Pasillo (x = 4.5, puerta 1.8 a 2.6).
        wall(4.5, 0.0, 4.5, 1.8)
        wall(4.5, 2.6, 4.5, 3.0)
        // Sala | Cocina (x = 4.5, puerta 4.6 a 5.4).
        wall(4.5, 3.0, 4.5, 4.6)
        wall(4.5, 5.4, 4.5, 7.0)
        // Cocina | Pasillo (y = 3, puerta variable según la versión).
        wall(4.5, 3.0, kitchenDoor.first, 3.0)
        wall(kitchenDoor.second, 3.0, 10.0, 3.0)
        // Baño (y = 1.5 con puerta 8.4 a 9.0; x = 7.5).
        wall(7.5, 1.5, 8.4, 1.5)
        wall(9.0, 1.5, 10.0, 1.5)
        wall(7.5, 0.0, 7.5, 1.5)

        return FloorPlan(
            mapVersionId = version,
            widthM = 10.0,
            heightM = 7.0,
            semantic = true,
            rooms = listOf(
                Room("Sala", PlanRect(0.0, 3.0, 4.5, 7.0)),
                Room("Cocina", PlanRect(4.5, 3.0, 10.0, 7.0)),
                Room("Recámara", PlanRect(0.0, 0.0, 4.5, 3.0)),
                Room("Pasillo", PlanRect(4.5, 1.5, 10.0, 3.0)),
                Room("Baño", PlanRect(7.5, 0.0, 10.0, 1.5)),
            ),
            walls = walls,
            stations = stations(version).map { it.station },
            unexplored = listOf(PlanRect(4.5, 0.0, 7.5, 1.5)),
        )
    }

    /** Tramo de la ruta. [stationIndex] indica que al terminar el tramo el robot está en esa estación. */
    class Leg(val from: PlanPoint, val to: PlanPoint, val stationIndex: Int?) {
        val length: Double = hypot(to.xM - from.xM, to.yM - from.yM)
    }

    /**
     * Ruta de un recorrido completo, desde la base y de regreso a ella, sin las estaciones omitidas.
     */
    fun route(version: MapVersionId, skipped: Set<Int>): List<Leg> {
        val points = mutableListOf<Pair<PlanPoint, Int?>>(DOCK to null)
        stations(version).forEachIndexed { index, spec ->
            if (index in skipped) return@forEachIndexed
            spec.approach.forEachIndexed { i, p ->
                points += p to (if (i == spec.approach.lastIndex) index else null)
            }
            spec.approach.dropLast(1).reversed().forEach { points += it to null }
        }
        points += PlanPoint(5.2, SPINE_Y) to null
        points += DOCK to null
        return points.zipWithNext { a, b -> Leg(a.first, b.first, b.second) }
            .filter { it.length > 1e-6 || it.stationIndex != null }
    }
}
