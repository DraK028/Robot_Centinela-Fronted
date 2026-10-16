package com.robotcentinela.app.shell.demo

import com.robotcentinela.app.environmentalmonitoring.domain.ChannelKey
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingQuality
import com.robotcentinela.app.environmentalmonitoring.domain.ReadingValue
import com.robotcentinela.app.environmentalmonitoring.domain.SensorReading
import com.robotcentinela.app.homemapping.domain.MapVersionId
import com.robotcentinela.app.homemapping.domain.Pose
import com.robotcentinela.app.identity.domain.Home
import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.incidents.domain.Alert
import com.robotcentinela.app.incidents.domain.AlertId
import com.robotcentinela.app.incidents.domain.AlertOrigin
import com.robotcentinela.app.incidents.domain.AlertRuleId
import com.robotcentinela.app.incidents.domain.AlertState
import com.robotcentinela.app.missions.domain.MissionEnergy
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.missions.domain.MissionStatus
import com.robotcentinela.app.missions.domain.MissionSummary
import com.robotcentinela.app.missions.domain.StationOutcome
import com.robotcentinela.app.missions.domain.StationVisit
import com.robotcentinela.app.robothealth.domain.BatteryStatus
import com.robotcentinela.app.robothealth.domain.RobotReportedState
import com.robotcentinela.app.shared.DataAccessException
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.telemetry.domain.BootId
import com.robotcentinela.app.telemetry.domain.ClockQuality
import com.robotcentinela.app.telemetry.domain.DeviceId
import com.robotcentinela.app.telemetry.domain.FrameId
import com.robotcentinela.app.telemetry.domain.StreamVersion
import com.robotcentinela.app.telemetry.domain.TelemetryFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mundo simulado con el que trabajan todos los adaptadores de demostración (HU15, RF35).
 *
 * - Genera un frame en vivo cada 2 s (§7), con `stream_version` contiguo (§7.4), pose estimada,
 *   lecturas con calidad y batería solo con voltaje, como en `JSON_estructura.txt`.
 * - Genera de forma determinista 30 días de misiones históricas (dos por día), con huecos reales
 *   (días sin misión), estaciones omitidas con causa y una misión detenida localmente.
 * - Sus datos están rotulados como simulados en la interfaz y no salen del teléfono (HU15 CDA 3).
 *
 * Todos los valores son sintéticos. No describen ninguna vivienda ni dispositivo real.
 */
class DemoWorld(
    private val clock: Clock,
    scope: CoroutineScope,
    val zone: ZoneId = UtcInterval.DEFAULT_ZONE,
) {

    // --- Identidades sintéticas ---------------------------------------------------------------

    val mainHome = Home(HomeId(uuid("5a7e0000-0000-4000-8000-000000000001")), "Casa de prueba", zone)
    val emptyHome = Home(HomeId(uuid("5a7e0000-0000-4000-8000-000000000002")), "Departamento sin robot", zone)
    val robot = Robot(RobotId(uuid("5a7e0000-0000-4000-8000-0000000000a1")), mainHome.id, "RC-01", "active")

    private val deviceId = DeviceId(uuid("5a7e0000-0000-4000-8000-0000000000d1"))
    private val historicalBoot = BootId(uuid("5a7e0000-0000-4000-8000-0000000000b0"))
    private val liveBoot = BootId(UUID.randomUUID())

    // --- Escenario ---------------------------------------------------------------------------

    private val mutableScenario = MutableStateFlow(DemoScenario.Complete)

    /** Escenario activo del simulador. */
    val scenario: StateFlow<DemoScenario> = mutableScenario.asStateFlow()

    @Volatile
    private var scenarioChangedAt: Instant = clock.instant()

    fun setScenario(value: DemoScenario) {
        if (mutableScenario.value == value) return
        scenarioChangedAt = clock.instant()
        mutableScenario.value = value
    }

    /** Lanza un error de red simulado si el teléfono está "sin conexión" (escenario de error). */
    internal fun requireOnline(operation: String) {
        if (mutableScenario.value == DemoScenario.ClientOffline) {
            throw DataAccessException("$operation: sin conexión de red (simulada)")
        }
    }

    // --- Misiones históricas -----------------------------------------------------------------

    private class HistoricalMission(
        val index: Int,
        val id: MissionId,
        val start: Instant,
        val version: MapVersionId,
        val skipped: Map<Int, String>,
        val stoppedLocally: Boolean,
        val motion: DemoMotion,
        val frameCount: Int,
        val firstStreamVersion: Long,
    ) {
        val durationS: Double get() = (frameCount - 1).coerceAtLeast(0) * HISTORY_PERIOD_S
        val end: Instant get() = start.plusMillis((durationS * 1000).roundToLong())
    }

    val startedAt: Instant = clock.instant()

    private val historical: List<HistoricalMission> = buildHistory()

    /** Última versión histórica; los frames en vivo continúan desde aquí sin huecos. */
    private val lastHistoricalVersion: Long =
        historical.lastOrNull()?.let { it.firstStreamVersion + it.frameCount - 1 } ?: 0L

    private fun buildHistory(): List<HistoricalMission> {
        val today = startedAt.atZone(zone).toLocalDate()
        val starts = mutableListOf<Instant>()
        for (daysAgo in HISTORY_DAYS downTo 0) {
            val date = today.minusDays(daysAgo.toLong())
            for (time in listOf(LocalTime.of(10, 0), LocalTime.of(19, 0))) {
                val start = date.atTime(time).atZone(zone).toInstant()
                if (start.plusSeconds(15 * 60) < startedAt) starts += start
            }
        }
        var nextVersion = 1L
        val missions = mutableListOf<HistoricalMission>()
        starts.forEachIndexed { index, start ->
            // Huecos reales: algunos turnos no hubo misión (robot sin carga o apagado).
            if (index % 9 == 4) return@forEachIndexed
            val daysAgo = Duration.between(start, startedAt).toDays()
            val version = when {
                daysAgo > 25 -> DemoPlans.MAP_LEGACY
                daysAgo > 12 -> DemoPlans.MAP_V1
                else -> DemoPlans.MAP_V2
            }
            val skipped = buildMap {
                if (index % 5 == 2) put(4, "Puerta del baño cerrada")
                if (index % 7 == 3) put(0, "Obstáculo en la ruta")
            }
            val stopped = index % 11 == 6
            val motion = DemoMotion(version, skipped.keys)
            val duration = if (stopped) motion.durationS * 0.45 else motion.durationS
            val frames = (duration / HISTORY_PERIOD_S).toInt() + 1
            missions += HistoricalMission(
                index = index,
                id = MissionId(UUID(MISSION_ID_HIGH, index.toLong())),
                start = start,
                version = version,
                skipped = skipped,
                stoppedLocally = stopped,
                motion = motion,
                frameCount = frames,
                firstStreamVersion = nextVersion,
            )
            nextVersion += frames
        }
        return missions
    }

    private fun historicalFrame(mission: HistoricalMission, k: Int): TelemetryFrame {
        val elapsed = k * HISTORY_PERIOD_S
        val captured = mission.start.plusMillis((elapsed * 1000).roundToLong())
        val state = mission.motion.at(elapsed)
        val random = Random(mission.index * 100_003L + k)
        val voltage = 12.3 - 0.8 * (elapsed / mission.motion.durationS)
        return buildFrame(
            id = FrameId(UUID(FRAME_ID_HIGH + mission.index, k.toLong())),
            bootId = historicalBoot,
            sequence = k.toLong(),
            capturedAt = captured,
            monotonicMs = (elapsed * 1000).roundToLong(),
            missionId = mission.id,
            mapVersionId = mission.version,
            state = state,
            readings = completeReadings(state, random, captured, warming = elapsed < 60.0, dropTemperature = k % 37 == 5),
            battery = BatteryStatus(voltage, null, null, "voltage_only"),
            receivedAt = captured.plusMillis(300 + random.nextLong(400)),
            streamVersion = mission.firstStreamVersion + k,
        )
    }

    // --- Frames en vivo ----------------------------------------------------------------------

    private val lock = Any()
    private val live = ArrayDeque<TelemetryFrame>()
    private var nextLiveVersion = lastHistoricalVersion + 1
    private var liveSequence = 0L
    private val liveMotion = DemoMotion(DemoPlans.MAP_V2, emptySet())
    private val commits = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    /** Avisos de commit de frames nuevos (lo que Realtime avisaría, sin datos, RB26). */
    internal val committed: SharedFlow<Unit> = commits.asSharedFlow()

    init {
        scope.launch {
            while (true) {
                if (mutableScenario.value != DemoScenario.IngestInterrupted) produceLiveFrame()
                delay(LIVE_PERIOD_MS)
            }
        }
    }

    private fun liveElapsedS(at: Instant): Double = Duration.between(startedAt, at).toMillis() / 1000.0

    private fun liveMissionIndex(at: Instant): Int = (liveElapsedS(at) / liveMotion.durationS).toInt()

    private fun liveMissionId(index: Int) = MissionId(UUID(LIVE_MISSION_ID_HIGH, index.toLong()))

    private fun produceLiveFrame() {
        val now = clock.instant()
        val elapsed = liveElapsedS(now)
        val missionIndex = liveMissionIndex(now)
        val state = liveMotion.at(elapsed - missionIndex * liveMotion.durationS)
        val random = Random(now.toEpochMilli())
        val scenarioNow = mutableScenario.value
        val hours = elapsed / 3600.0
        synchronized(lock) {
            buildFrame(
                id = FrameId(UUID.randomUUID()),
                bootId = liveBoot,
                sequence = liveSequence++,
                capturedAt = now,
                monotonicMs = (elapsed * 1000).roundToLong(),
                missionId = liveMissionId(missionIndex),
                mapVersionId = DemoPlans.MAP_V2,
                state = state,
                readings = if (scenarioNow == DemoScenario.Degraded) {
                    degradedReadings(random)
                } else {
                    completeReadings(state, random, now, warming = false, dropTemperature = false)
                },
                battery = if (scenarioNow == DemoScenario.Degraded) {
                    BatteryStatus(null, null, null, "voltage_only")
                } else {
                    BatteryStatus(max(10.9, 11.6 - 0.3 * hours), null, null, "voltage_only")
                },
                receivedAt = now.plusMillis(250 + random.nextLong(300)),
                streamVersion = nextLiveVersion++,
            ).also {
                live.addLast(it)
                while (live.size > LIVE_RETENTION) live.removeFirst()
            }
        }
        commits.tryEmit(Unit)
    }

    // --- Consultas usadas por los adaptadores --------------------------------------------------

    /** Último frame conocido (snapshot). */
    internal fun latestFrame(): TelemetryFrame? = synchronized(lock) { live.lastOrNull() }
        ?: historical.lastOrNull()?.let { historicalFrame(it, it.frameCount - 1) }

    /** Frames en vivo posteriores a [after], en orden ascendente. */
    internal fun liveFramesAfter(after: StreamVersion, limit: Int): List<TelemetryFrame> = synchronized(lock) {
        live.asSequence().filter { it.streamVersion > after }.take(limit).toList()
    }

    /** Frames capturados en [interval], de una misión opcional, en orden ascendente de captura. */
    internal fun framesBetween(interval: UtcInterval, missionId: MissionId?): Sequence<TelemetryFrame> {
        val past = historical.asSequence()
            .filter { missionId == null || it.id == missionId }
            .filter { it.start < interval.endExclusive && !it.end.isBefore(interval.start) }
            .flatMap { mission -> (0 until mission.frameCount).asSequence().map { historicalFrame(mission, it) } }
        val recent = synchronized(lock) { live.toList() }.asSequence()
            .filter { missionId == null || it.missionId == missionId }
        return (past + recent).filter { (it.capturedAt ?: it.receivedAt) in interval }
    }

    // --- Misiones ----------------------------------------------------------------------------

    internal fun missions(): List<MissionSummary> {
        val now = clock.instant()
        val currentIndex = liveMissionIndex(now)
        val liveMissions = (0..currentIndex).map { index ->
            val start = startedAt.plusMillis((index * liveMotion.durationS * 1000).roundToLong())
            val running = index == currentIndex
            val elapsed = if (running) Duration.between(start, now).toMillis() / 1000.0 else liveMotion.durationS
            val reached = liveMotion.at(elapsed).reached.toSet()
            summary(
                id = liveMissionId(index),
                version = DemoPlans.MAP_V2,
                start = start,
                end = if (running) null else start.plusMillis((liveMotion.durationS * 1000).roundToLong()),
                status = if (running) MissionStatus.Running else MissionStatus.Completed,
                motion = liveMotion,
                reached = reached,
                skipped = emptyMap(),
                pendingAfterStop = null,
                startVoltage = 11.6,
                endVoltage = if (running) null else 11.5,
            )
        }
        val past = historical.map { mission ->
            val reached = mission.motion.at(mission.durationS).reached.toSet()
            summary(
                id = mission.id,
                version = mission.version,
                start = mission.start,
                end = mission.end,
                status = if (mission.stoppedLocally) MissionStatus.Aborted else MissionStatus.Completed,
                motion = mission.motion,
                reached = reached,
                skipped = mission.skipped,
                pendingAfterStop = if (mission.stoppedLocally) "Misión detenida localmente en el robot" else null,
                startVoltage = 12.3,
                endVoltage = 12.3 - 0.8 * (mission.durationS / mission.motion.durationS),
            )
        }
        return (past + liveMissions).sortedByDescending { it.startedAt }
    }

    private fun summary(
        id: MissionId,
        version: MapVersionId,
        start: Instant,
        end: Instant?,
        status: MissionStatus,
        motion: DemoMotion,
        reached: Set<Int>,
        skipped: Map<Int, String>,
        pendingAfterStop: String?,
        startVoltage: Double,
        endVoltage: Double?,
    ): MissionSummary {
        val stations = DemoPlans.stations(version).mapIndexed { index, spec ->
            val outcome = when {
                index in reached -> StationOutcome.Visited(
                    motion.arrivals[index]?.let { start.plusMillis((it * 1000).roundToLong()) },
                )
                index in skipped -> StationOutcome.Omitted(skipped.getValue(index))
                pendingAfterStop != null -> StationOutcome.Omitted(pendingAfterStop)
                else -> StationOutcome.Pending
            }
            StationVisit(spec.station.id, spec.station.name, outcome)
        }
        val hours = ((end ?: clock.instant()).toEpochMilli() - start.toEpochMilli()) / 3_600_000.0
        return MissionSummary(
            id = id,
            robotId = robot.id,
            mapVersionId = version,
            startedAt = start,
            endedAt = end,
            status = status,
            stations = stations,
            energy = MissionEnergy(
                startVoltageV = startVoltage,
                endVoltageV = endVoltage,
                consumedWh = if (end != null) hours * AVERAGE_POWER_W else null,
                method = "estimated",
            ),
        )
    }

    // --- Alertas (persistidas por el backend; la app solo las lee) ------------------------------

    internal fun alerts(): List<Alert> {
        val list = mutableListOf<Alert>()
        var n = 0L

        fun add(
            type: String,
            severity: String,
            origin: AlertOrigin,
            state: AlertState,
            opened: Instant,
            lastSeen: Instant,
            resolved: Instant?,
        ) {
            list += Alert(
                id = AlertId(UUID(ALERT_ID_HIGH, n++)),
                robotId = robot.id,
                ruleId = if (origin == AlertOrigin.BackendRule) AlertRuleId(UUID(RULE_ID_HIGH, type.hashCode().toLong())) else null,
                type = type,
                severity = severity,
                state = state,
                origin = origin,
                openedAt = opened,
                lastSeenAt = lastSeen,
                resolvedAt = resolved,
            )
        }

        historical.forEach { mission ->
            if (mission.index % 6 == 1) {
                val opened = mission.start.plusSeconds(260)
                add("gas_signal_high", "warning", AlertOrigin.BackendRule, AlertState.Resolved, opened, opened.plusSeconds(150), opened.plusSeconds(420))
            }
            if (mission.skipped.isNotEmpty()) {
                val opened = mission.start.plusSeconds(400)
                add("station_skipped", "info", AlertOrigin.DeviceReported, AlertState.Resolved, opened, opened, opened.plusSeconds(60))
            }
            if (mission.stoppedLocally) {
                val opened = mission.end
                add("mission_stopped", "info", AlertOrigin.DeviceReported, AlertState.Acknowledged, opened, opened, null)
            }
        }
        val threeDays = startedAt.minus(Duration.ofDays(3)).minusSeconds(5_400)
        add("ingest_interrupted", "warning", AlertOrigin.BackendRule, AlertState.Resolved, threeDays, threeDays.plusSeconds(240), threeDays.plusSeconds(250))
        val nineDays = startedAt.minus(Duration.ofDays(9)).minusSeconds(3_600)
        add("battery_low", "warning", AlertOrigin.DeviceReported, AlertState.Acknowledged, nineDays, nineDays.plusSeconds(900), null)
        val calibration = startedAt.minusSeconds(2 * 3_600)
        add("calibration_pending", "warning", AlertOrigin.DeviceReported, AlertState.Open, calibration, startedAt.minusSeconds(60), null)

        val changed = scenarioChangedAt
        val now = clock.instant()
        when (mutableScenario.value) {
            DemoScenario.Degraded ->
                add("sensor_fault", "critical", AlertOrigin.DeviceReported, AlertState.Open, changed.plusSeconds(2), now, null)
            DemoScenario.IngestInterrupted -> if (Duration.between(changed, now).seconds >= 10) {
                add("ingest_interrupted", "warning", AlertOrigin.BackendRule, AlertState.Open, changed.plusSeconds(10), now, null)
            }
            else -> Unit
        }
        return list.sortedByDescending { it.openedAt }
    }

    // --- Construcción de frames ----------------------------------------------------------------

    private fun buildFrame(
        id: FrameId,
        bootId: BootId,
        sequence: Long,
        capturedAt: Instant,
        monotonicMs: Long,
        missionId: MissionId,
        mapVersionId: MapVersionId,
        state: DemoMotion.State,
        readings: List<SensorReading>,
        battery: BatteryStatus,
        receivedAt: Instant,
        streamVersion: Long,
    ) = TelemetryFrame(
        id = id,
        schemaVersion = "1.1",
        deviceId = deviceId,
        bootId = bootId,
        sequence = sequence,
        capturedAt = capturedAt,
        monotonicMs = monotonicMs,
        clockQuality = ClockQuality.Synced,
        missionId = missionId,
        mapVersionId = mapVersionId,
        pose = Pose(
            xM = state.xM,
            yM = state.yM,
            thetaRad = state.thetaRad,
            sigmaPositionM = min(0.35, 0.05 + 0.02 * state.metersSinceStation),
            quality = "estimated",
        ),
        readings = readings,
        battery = battery,
        robotState = RobotReportedState(
            when {
                state.finished -> "docked"
                state.sampling -> "sampling"
                else -> "moving"
            },
        ),
        motorsActive = !state.sampling && !state.finished,
        firmwareVersion = "0.2.0",
        receivedAt = receivedAt,
        streamVersion = StreamVersion(streamVersion),
    )

    private fun completeReadings(
        state: DemoMotion.State,
        random: Random,
        at: Instant,
        warming: Boolean,
        dropTemperature: Boolean,
    ): List<SensorReading> {
        val kitchen = max(0.0, 1.0 - hypot(state.xM - 7.6, state.yM - 5.2) / 4.0)
        val dayPhase = at.atZone(zone).toLocalTime().toSecondOfDay() / 86_400.0
        val mq6 = (1_700 + 320 * kitchen + random.nextDouble(-25.0, 25.0)).roundToLong().toDouble()
        val mq4 = (1_400 + 90 * kitchen + random.nextDouble(-20.0, 20.0)).roundToLong().toDouble()
        val temperature = round1(23.6 + 1.4 * sin(2 * PI * (dayPhase - 0.3)) + random.nextDouble(-0.1, 0.1))
        val humidity = round1(48.0 + 3.0 * sin(2 * PI * dayPhase) + random.nextDouble(-0.4, 0.4))
        return listOf(
            reading(MQ6, ReadingValue.Numeric(mq6), if (warming) ReadingQuality.Warming else ReadingQuality.Uncalibrated),
            reading(MQ4, ReadingValue.Numeric(mq4), if (warming) ReadingQuality.Warming else ReadingQuality.Uncalibrated),
            reading(MQ7, ReadingValue.Absent, ReadingQuality.NotInstalled),
            reading(
                TEMPERATURE,
                if (dropTemperature) ReadingValue.Absent else ReadingValue.Numeric(temperature),
                ReadingQuality.Valid,
            ),
            reading(HUMIDITY, ReadingValue.Numeric(humidity), ReadingQuality.Valid),
            reading(FRONT_DISTANCE, ReadingValue.Numeric(round2(0.35 + random.nextDouble(0.0, 1.4))), ReadingQuality.Valid),
            reading(IR_FRONT_LEFT, ReadingValue.BooleanValue(true), ReadingQuality.Valid),
            reading(IR_FRONT_RIGHT, ReadingValue.BooleanValue(true), ReadingQuality.Valid),
            reading(IR_REAR_LEFT, ReadingValue.BooleanValue(true), ReadingQuality.Valid),
            reading(IR_REAR_RIGHT, ReadingValue.BooleanValue(true), ReadingQuality.Valid),
        )
    }

    /** Lecturas del escenario degradado, como el fixture `frame_qa_sensor_states.json`. */
    private fun degradedReadings(random: Random): List<SensorReading> = listOf(
        reading(MQ6, ReadingValue.Numeric(2_100.0 + random.nextInt(-20, 20)), ReadingQuality.Warming),
        reading(MQ4, ReadingValue.Absent, ReadingQuality.Fault),
        reading(MQ7, ReadingValue.Absent, ReadingQuality.NotInstalled),
        reading(TEMPERATURE, ReadingValue.Absent, ReadingQuality.Valid),
        reading(HUMIDITY, ReadingValue.Numeric(100.0), ReadingQuality.OutOfRange),
        reading(FRONT_DISTANCE, ReadingValue.Numeric(0.45), ReadingQuality.fromRaw("calibrating")),
        reading(IR_FRONT_LEFT, ReadingValue.Absent, ReadingQuality.Fault),
        reading(IR_FRONT_RIGHT, ReadingValue.BooleanValue(false), ReadingQuality.Valid),
        reading(IR_REAR_LEFT, ReadingValue.BooleanValue(true), ReadingQuality.Valid),
        reading(IR_REAR_RIGHT, ReadingValue.BooleanValue(true), ReadingQuality.Valid),
    )

    private fun reading(key: ChannelKey, value: ReadingValue, quality: ReadingQuality) =
        SensorReading(key, value, quality, calibrationId = null)

    companion object {
        val MQ6 = ChannelKey("mq6_raw")
        val MQ4 = ChannelKey("mq4_raw")
        val MQ7 = ChannelKey("mq7_raw")
        val TEMPERATURE = ChannelKey("temperature")
        val HUMIDITY = ChannelKey("humidity")
        val FRONT_DISTANCE = ChannelKey("front_distance_m")
        val IR_FRONT_LEFT = ChannelKey("ir_floor_front_left")
        val IR_FRONT_RIGHT = ChannelKey("ir_floor_front_right")
        val IR_REAR_LEFT = ChannelKey("ir_floor_rear_left")
        val IR_REAR_RIGHT = ChannelKey("ir_floor_rear_right")

        /** Un frame cada 2 s, como el firmware (§7). */
        const val LIVE_PERIOD_MS = 2_000L

        /** El historial sintético se muestrea cada 6 s para no generar miles de filas. */
        const val HISTORY_PERIOD_S = 6.0
        const val HISTORY_DAYS = 30

        /** Frames en vivo retenidos (90 min a 2 s, §7.5). Más atrás, la sincronización ve un hueco. */
        const val LIVE_RETENTION = 2_700

        private const val AVERAGE_POWER_W = 9.5
        private const val MISSION_ID_HIGH = 0x5A7E_4D15_0000_0000L
        private const val LIVE_MISSION_ID_HIGH = 0x5A7E_4D15_FFFF_0000L
        private const val FRAME_ID_HIGH = 0x5A7E_F4A3_0000_0000L
        private const val ALERT_ID_HIGH = 0x5A7E_A1E7_0000_0000L
        private const val RULE_ID_HIGH = 0x5A7E_4B1E_0000_0000L

        private fun uuid(text: String): UUID = UUID.fromString(text)
        private fun round1(v: Double) = Math.round(v * 10.0) / 10.0
        private fun round2(v: Double) = Math.round(v * 100.0) / 100.0
    }
}
