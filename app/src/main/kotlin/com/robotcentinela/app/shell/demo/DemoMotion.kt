package com.robotcentinela.app.shell.demo

import com.robotcentinela.app.homemapping.domain.MapVersionId
import kotlin.math.atan2
import kotlin.math.min

/**
 * Movimiento sintético del robot sobre una ruta de [DemoPlans] (HU15).
 *
 * Avanza a velocidad constante y se detiene [DWELL_S] segundos en cada estación para muestrear.
 */
internal class DemoMotion(version: MapVersionId, skipped: Set<Int>) {

    /** Posición del robot en un instante del recorrido. */
    data class State(
        val xM: Double,
        val yM: Double,
        val thetaRad: Double,
        /** `true` mientras el robot está detenido muestreando en una estación. */
        val sampling: Boolean,
        /** Estaciones ya alcanzadas (índices del recorrido completo), en orden. */
        val reached: List<Int>,
        /** Metros recorridos desde la última estación; la incertidumbre crece con ellos. */
        val metersSinceStation: Double,
        val finished: Boolean,
    )

    private val legs = DemoPlans.route(version, skipped)

    /** Duración total del recorrido, en segundos. */
    val durationS: Double = legs.sumOf { it.length / SPEED_M_S + (if (it.stationIndex != null) DWELL_S else 0.0) }

    /** Segundos desde el inicio hasta la llegada a cada estación del recorrido. */
    val arrivals: Map<Int, Double> = buildMap {
        var t = 0.0
        for (leg in legs) {
            t += leg.length / SPEED_M_S
            val station = leg.stationIndex
            if (station != null) {
                put(station, t)
                t += DWELL_S
            }
        }
    }

    fun at(elapsedS: Double): State {
        var remaining = elapsedS.coerceAtLeast(0.0)
        val reached = mutableListOf<Int>()
        var sinceStation = 0.0
        var heading = 0.0
        for (leg in legs) {
            val travel = leg.length / SPEED_M_S
            if (leg.length > 1e-6) heading = atan2(leg.to.yM - leg.from.yM, leg.to.xM - leg.from.xM)
            if (remaining < travel) {
                val f = if (travel > 0) remaining / travel else 1.0
                return State(
                    xM = leg.from.xM + (leg.to.xM - leg.from.xM) * f,
                    yM = leg.from.yM + (leg.to.yM - leg.from.yM) * f,
                    thetaRad = heading,
                    sampling = false,
                    reached = reached,
                    metersSinceStation = sinceStation + leg.length * f,
                    finished = false,
                )
            }
            remaining -= travel
            sinceStation += leg.length
            val station = leg.stationIndex
            if (station != null) {
                reached += station
                sinceStation = 0.0
                if (remaining < DWELL_S) {
                    return State(leg.to.xM, leg.to.yM, heading, true, reached, 0.0, finished = false)
                }
                remaining -= DWELL_S
            }
        }
        val end = legs.lastOrNull()?.to ?: DemoPlans.DOCK
        return State(end.xM, end.yM, heading, false, reached, min(sinceStation, 0.0), finished = true)
    }

    companion object {
        const val SPEED_M_S = 0.12
        const val DWELL_S = 30.0
    }
}
