package com.robotcentinela.app.telemetry.adapters.supabase

import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.shell.supabase.SupabaseResources
import com.robotcentinela.app.telemetry.ports.TelemetryChangeNotifier
import com.robotcentinela.app.telemetry.ports.TelemetryNotice
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adaptador de [TelemetryChangeNotifier] con Supabase Realtime (Postgres Changes).
 *
 * Se suscribe a las inserciones de [SupabaseResources.TELEMETRY_FRAMES_TABLE] filtradas por
 * `robot_id`. El expediente indica que en el MVP basta Postgres Changes en tablas seleccionadas
 * (§7.4). El aviso **no** se usa como dato: solo dispara una consulta de deltas (RB26). Realtime
 * respeta las políticas RLS de la tabla.
 */
class SupabaseTelemetryChangeNotifier(private val supabase: SupabaseClient) : TelemetryChangeNotifier {

    override fun notices(robotId: RobotId): Flow<TelemetryNotice> = channelFlow {
        val channel = supabase.channel("telemetry-${robotId.value}")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = SupabaseResources.SCHEMA) {
            table = SupabaseResources.TELEMETRY_FRAMES_TABLE
            filter(SupabaseResources.Columns.ROBOT_ID, FilterOperator.EQ, robotId.value.toString())
        }

        launch { inserts.collect { send(TelemetryNotice.FrameCommitted) } }
        launch {
            var wasSubscribed = false
            channel.status.collect { status ->
                when (status) {
                    RealtimeChannel.Status.SUBSCRIBED -> {
                        wasSubscribed = true
                        send(TelemetryNotice.Subscribed)
                    }
                    RealtimeChannel.Status.UNSUBSCRIBED -> if (wasSubscribed) {
                        wasSubscribed = false
                        send(TelemetryNotice.Interrupted("Canal Realtime desconectado"))
                    }
                    else -> Unit
                }
            }
        }

        try {
            channel.subscribe()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }
}
