package com.robotcentinela.app.telemetry.adapters.demo

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.shell.demo.DemoScenario
import com.robotcentinela.app.shell.demo.DemoWorld
import com.robotcentinela.app.telemetry.domain.FrameRecord
import com.robotcentinela.app.telemetry.domain.StreamVersion
import com.robotcentinela.app.telemetry.ports.TelemetryChangeNotifier
import com.robotcentinela.app.telemetry.ports.TelemetryNotice
import com.robotcentinela.app.telemetry.ports.TelemetryQueryPort
import com.robotcentinela.app.telemetry.ports.TelemetrySnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Consulta de frames sobre el mundo simulado (HU15). Mismo contrato que el adaptador de Supabase. */
class DemoTelemetryQueryAdapter(private val world: DemoWorld) : TelemetryQueryPort {

    override suspend fun snapshot(robotId: RobotId): TelemetrySnapshot {
        world.requireOnline("snapshot de telemetría")
        if (robotId != world.robot.id) return TelemetrySnapshot(emptyList(), null)
        val latest = world.latestFrame() ?: return TelemetrySnapshot(emptyList(), null)
        return TelemetrySnapshot(listOf(FrameRecord.Interpreted(latest)), latest.streamVersion)
    }

    override suspend fun framesAfter(robotId: RobotId, after: StreamVersion, limit: Int): List<FrameRecord> {
        world.requireOnline("deltas de telemetría")
        if (robotId != world.robot.id) return emptyList()
        return world.liveFramesAfter(after, limit).map { FrameRecord.Interpreted(it) }
    }

    override suspend fun framesBetween(
        robotId: RobotId,
        interval: UtcInterval,
        missionId: MissionId?,
        page: PageRequest,
    ): List<FrameRecord> {
        world.requireOnline("historial de telemetría")
        if (robotId != world.robot.id) return emptyList()
        return world.framesBetween(interval, missionId)
            .drop(page.offset.toInt())
            .take(page.limit)
            .map { FrameRecord.Interpreted(it) }
            .toList()
    }
}

/**
 * Avisos simulados de Realtime: confirman la suscripción y avisan cada commit, sin datos (RB26).
 *
 * En el escenario [DemoScenario.ClientOffline] la suscripción se interrumpe hasta que el escenario
 * cambie; entonces se confirma de nuevo y el sincronizador reconcilia desde su última versión (RB28).
 */
class DemoTelemetryChangeNotifier(private val world: DemoWorld) : TelemetryChangeNotifier {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun notices(robotId: RobotId): Flow<TelemetryNotice> =
        world.scenario
            .map { it == DemoScenario.ClientOffline }
            .distinctUntilChanged()
            .flatMapLatest { offline ->
                flow {
                    if (offline) {
                        emit(TelemetryNotice.Interrupted("Sin conexión de red (simulada)"))
                        awaitCancellation()
                    } else {
                        emit(TelemetryNotice.Subscribed)
                        world.committed.collect { emit(TelemetryNotice.FrameCommitted) }
                    }
                }
            }
}
