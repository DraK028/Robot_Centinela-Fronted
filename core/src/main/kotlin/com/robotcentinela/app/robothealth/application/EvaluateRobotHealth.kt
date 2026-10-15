package com.robotcentinela.app.robothealth.application

import com.robotcentinela.app.environmentalmonitoring.domain.DataFreshness
import com.robotcentinela.app.environmentalmonitoring.domain.FreshnessPolicy
import com.robotcentinela.app.robothealth.domain.BatteryInterpretation
import com.robotcentinela.app.robothealth.domain.IngestConnectivityPolicy
import com.robotcentinela.app.robothealth.domain.IngestStatus
import com.robotcentinela.app.robothealth.domain.RobotReportedState
import com.robotcentinela.app.telemetry.domain.CaptureStamp
import com.robotcentinela.app.telemetry.domain.LatestTelemetryProjection
import java.time.Clock

/**
 * Datos del panel operativo del robot (RF06, RF18, RF19, RNF04).
 *
 * @property battery batería con su método (HU08).
 * @property reportedState estado que reporta el dispositivo, como texto original (RF19).
 * @property motorsActive orden o estado reportado de los motores (no prueba giro efectivo).
 * @property ingest estado de la ingesta. "Interrumpida" no significa robot apagado (RF29).
 * @property lastFrameFreshness edad del último frame. Un dato atrasado no se anima como si fuera
 * actual (RF12).
 */
data class RobotHealthReport(
    val battery: BatteryInterpretation,
    val reportedState: RobotReportedState?,
    val motorsActive: Boolean?,
    val firmwareVersion: String?,
    val ingest: IngestStatus,
    val lastFrameFreshness: DataFreshness?,
)

/** Caso de uso: evaluar el estado operativo del robot a partir de su último frame (HU08, RNF04). */
class EvaluateRobotHealth(
    private val connectivityPolicy: IngestConnectivityPolicy,
    private val freshnessPolicy: FreshnessPolicy,
    private val clock: Clock,
) {

    operator fun invoke(projection: LatestTelemetryProjection): RobotHealthReport {
        val now = clock.instant()
        val frame = projection.latestFrame
        return RobotHealthReport(
            battery = BatteryInterpretation.of(frame?.battery),
            reportedState = frame?.robotState,
            motorsActive = frame?.motorsActive,
            firmwareVersion = frame?.firmwareVersion,
            ingest = connectivityPolicy.evaluate(projection.lastReceivedAt, now),
            lastFrameFreshness = frame?.let {
                freshnessPolicy.evaluate(CaptureStamp.of(it).referenceInstant, null, now)
            },
        )
    }
}
