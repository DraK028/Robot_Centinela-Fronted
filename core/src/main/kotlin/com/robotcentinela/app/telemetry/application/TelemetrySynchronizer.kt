package com.robotcentinela.app.telemetry.application

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.telemetry.domain.FrameId
import com.robotcentinela.app.telemetry.domain.FrameRecord
import com.robotcentinela.app.telemetry.domain.StreamVersion
import com.robotcentinela.app.telemetry.domain.TelemetryFrame
import com.robotcentinela.app.telemetry.ports.TelemetryChangeNotifier
import com.robotcentinela.app.telemetry.ports.TelemetryNotice
import com.robotcentinela.app.telemetry.ports.TelemetryQueryPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Caso de uso: sincronizar en vivo la telemetría de un robot (HU03, RF07, RF08, RB19, RB26 a RB28).
 *
 * Aplica el algoritmo del expediente (§7.4), detallado en
 * `docs/architecture/sincronizacion-telemetria.md`:
 * 1. Abre la suscripción y espera su confirmación ([TelemetryNotice.Subscribed]).
 * 2. Pide un snapshot con watermark y lo aplica.
 * 3. Pide los deltas posteriores (`after_version`), paginados y ordenados por `stream_version`.
 * 4. Cada aviso de Realtime dispara una consulta de deltas; el aviso nunca trae datos (RB26).
 * 5. Al reconectar, continúa desde `lastAppliedVersion`. Si hay un hueco en el cursor, los frames
 *    intermedios se eliminaron por retención: aplica un snapshot completo y marca
 *    [TelemetrySyncState.historyLimited] (RB26, T08). Para detectar el hueco se apoya en que el
 *    backend incrementa `stream_version` de uno en uno por frame aceptado (§7.4, §13.6).
 * 6. Reconcilia periódicamente ([SyncConfig.reconciliationInterval]) para cubrir avisos omitidos.
 *
 * Garantías:
 * - Deduplica por `stream_version` y `frame_id`: un frame repetido no se aplica dos veces y no
 *   produce parpadeo ni puntos duplicados (HU03 CDA 3).
 * - Un aviso que llega antes de la respuesta HTTP no hace retroceder el estado, porque nunca se
 *   aplica una versión menor o igual a la última aplicada.
 *
 * Una instancia sincroniza un solo robot. Debe crearse para un robot del ámbito seleccionado
 * ([com.robotcentinela.app.identity.application.CurrentHomeScope]).
 */
class TelemetrySynchronizer(
    val robotId: RobotId,
    private val query: TelemetryQueryPort,
    private val notifier: TelemetryChangeNotifier,
    private val config: SyncConfig = SyncConfig(),
) {

    private val mutableState = MutableStateFlow(TelemetrySyncState())

    /** Estado observable: proyección del último dato, cursor y estado de conexión. */
    val state: StateFlow<TelemetrySyncState> = mutableState.asStateFlow()

    private val newFrames = MutableSharedFlow<TelemetryFrame>(extraBufferCapacity = 64)

    /**
     * Frames nuevos aplicados, en orden de `stream_version` y sin duplicados. Sirven para gráficas
     * y trayectorias.
     */
    val appliedFrames: SharedFlow<TelemetryFrame> = newFrames.asSharedFlow()

    private val recentFrameIds = RecentFrameIds(config.dedupeWindow)
    private var job: Job? = null

    /** Inicia (o reanuda) la sincronización en [scope]. No hace nada si ya está en curso. */
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch { run() }
    }

    /**
     * Detiene la sincronización, por ejemplo al pasar la app a segundo plano. Al reanudar con
     * [start] se reconcilia desde la última versión aplicada (RB28).
     */
    suspend fun stop() {
        job?.cancelAndJoin()
        job = null
        mutableState.update { it.copy(status = SyncStatus.Stopped) }
    }

    private suspend fun run(): Unit = coroutineScope {
        setStatus(if (state.value.lastAppliedVersion == null) SyncStatus.Connecting else SyncStatus.Reconciling)

        val notices = Channel<TelemetryNotice>(Channel.UNLIMITED)
        launch {
            while (true) {
                notifier.notices(robotId)
                    .catch { cause -> emit(TelemetryNotice.Interrupted(cause.message ?: "Suscripción interrumpida")) }
                    .collect { notices.send(it) }
                // Si el flujo de avisos termina, se reintenta la suscripción tras el intervalo.
                notices.send(TelemetryNotice.Interrupted("Suscripción finalizada"))
                delay(config.reconciliationInterval)
            }
        }

        var subscribed = false
        while (true) {
            // Sin avisos durante el intervalo: reconciliación periódica (§7.4).
            val first = withTimeoutOrNull(config.reconciliationInterval) { notices.receive() }
            var mustSync = first == null && subscribed

            // Se agrupan los avisos pendientes: varios avisos seguidos producen una sola consulta.
            var notice: TelemetryNotice? = first
            while (notice != null) {
                when (notice) {
                    TelemetryNotice.Subscribed -> {
                        subscribed = true
                        mustSync = true
                    }
                    TelemetryNotice.FrameCommitted -> if (subscribed) mustSync = true
                    is TelemetryNotice.Interrupted -> {
                        subscribed = false
                        mustSync = false
                        setStatus(SyncStatus.Offline(notice.reason))
                    }
                }
                notice = notices.tryReceive().getOrNull()
            }

            if (mustSync) synchronize()
        }
    }

    private suspend fun synchronize() {
        try {
            val status = state.value.status
            if (status != SyncStatus.Live && status != SyncStatus.Connecting) {
                setStatus(SyncStatus.Reconciling)
            }
            if (state.value.lastAppliedVersion == null) {
                applySnapshot(historyLimited = false)
            }
            catchUp()
            setStatus(SyncStatus.Live)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setStatus(SyncStatus.Error(e.message ?: e::class.simpleName ?: "Error de sincronización"))
        }
    }

    /** Pide deltas desde la última versión aplicada hasta que no queden más. */
    private suspend fun catchUp() {
        var snapshotTaken = false
        while (true) {
            val after = state.value.lastAppliedVersion ?: StreamVersion.NONE
            val page = query.framesAfter(robotId, after, config.deltaPageSize).sortedBy { it.streamVersion }
            val first = page.firstOrNull() ?: return

            if (first.streamVersion > after.next() && !snapshotTaken) {
                // Hueco en el cursor: los frames intermedios ya no existen (retención).
                applySnapshot(historyLimited = true)
                snapshotTaken = true
                continue
            }

            apply(page)
            val advanced = state.value.lastAppliedVersion != after
            if (page.size < config.deltaPageSize || !advanced) return
        }
    }

    private suspend fun applySnapshot(historyLimited: Boolean) {
        val snapshot = query.snapshot(robotId)
        apply(snapshot.records)
        val watermark = snapshot.watermark ?: snapshot.records.maxOfOrNull { it.streamVersion }
        mutableState.update { current ->
            val base = current.lastAppliedVersion ?: StreamVersion.NONE
            current.copy(
                watermark = watermark ?: current.watermark,
                lastAppliedVersion = if (watermark != null && watermark > base) watermark else base,
                historyLimited = current.historyLimited || historyLimited,
            )
        }
    }

    /** Aplica los registros en orden, sin retroceder y sin duplicar. */
    private suspend fun apply(records: List<FrameRecord>) {
        var current = state.value
        val applied = mutableListOf<TelemetryFrame>()

        for (record in records.sortedBy { it.streamVersion }) {
            val last = current.lastAppliedVersion
            if (last != null && record.streamVersion <= last) continue

            current = when (record) {
                is FrameRecord.Interpreted -> if (recentFrameIds.add(record.frame.id)) {
                    applied += record.frame
                    current.copy(projection = current.projection.apply(record.frame))
                } else {
                    current
                }
                is FrameRecord.Rejected -> current.copy(rejectedFrames = current.rejectedFrames + 1)
            }
            current = current.copy(lastAppliedVersion = record.streamVersion)
        }

        mutableState.value = current
        applied.forEach { newFrames.emit(it) }
    }

    private fun setStatus(status: SyncStatus) {
        mutableState.update { it.copy(status = status) }
    }
}

/** Conjunto acotado de `frame_id` recientes. Olvida primero los más antiguos. */
internal class RecentFrameIds(private val capacity: Int) {

    private val ids = LinkedHashSet<FrameId>()

    /** Devuelve `false` si el identificador ya estaba registrado. */
    fun add(id: FrameId): Boolean {
        if (!ids.add(id)) return false
        if (ids.size > capacity) {
            val iterator = ids.iterator()
            iterator.next()
            iterator.remove()
        }
        return true
    }
}
