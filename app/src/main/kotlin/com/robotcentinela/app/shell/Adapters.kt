package com.robotcentinela.app.shell

import com.robotcentinela.app.environmentalmonitoring.adapters.demo.DemoCapabilitiesAdapter
import com.robotcentinela.app.environmentalmonitoring.adapters.demo.DemoMetricAggregateAdapter
import com.robotcentinela.app.environmentalmonitoring.ports.CapabilitiesPort
import com.robotcentinela.app.environmentalmonitoring.ports.MetricAggregatePort
import com.robotcentinela.app.homemapping.adapters.demo.DemoFloorPlanAdapter
import com.robotcentinela.app.homemapping.ports.FloorPlanPort
import com.robotcentinela.app.identity.adapters.demo.DemoAuthAdapter
import com.robotcentinela.app.identity.adapters.demo.DemoHomeScopeAdapter
import com.robotcentinela.app.identity.adapters.supabase.SupabaseAuthAdapter
import com.robotcentinela.app.identity.adapters.supabase.SupabaseHomeScopeAdapter
import com.robotcentinela.app.identity.ports.AuthPort
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.incidents.adapters.demo.DemoAlertQueryAdapter
import com.robotcentinela.app.incidents.adapters.supabase.SupabaseAlertQueryAdapter
import com.robotcentinela.app.incidents.ports.AlertQueryPort
import com.robotcentinela.app.missions.adapters.demo.DemoMissionAdapter
import com.robotcentinela.app.missions.ports.MissionQueryPort
import com.robotcentinela.app.shell.demo.DemoWorld
import com.robotcentinela.app.shell.supabase.SupabaseClientFactory
import com.robotcentinela.app.shell.supabase.SupabaseConfig
import com.robotcentinela.app.telemetry.adapters.demo.DemoTelemetryChangeNotifier
import com.robotcentinela.app.telemetry.adapters.demo.DemoTelemetryQueryAdapter
import com.robotcentinela.app.telemetry.adapters.supabase.SupabaseTelemetryChangeNotifier
import com.robotcentinela.app.telemetry.adapters.supabase.SupabaseTelemetryQueryAdapter
import com.robotcentinela.app.telemetry.ports.TelemetryChangeNotifier
import com.robotcentinela.app.telemetry.ports.TelemetryQueryPort
import kotlinx.coroutines.CoroutineScope
import java.time.Clock

/** Origen de los datos que ve la interfaz. */
sealed interface DataSource {

    /** Supabase real (Auth, PostgREST, Realtime). */
    data object Supabase : DataSource

    /**
     * Simulador local (HU15). La interfaz lo rotula como "Datos simulados" en todas las pantallas
     * (HU15 CDA 2, RB29).
     */
    data class Simulated(val world: DemoWorld) : DataSource
}

/**
 * Conjunto de implementaciones de los puertos de `:core`.
 *
 * Los puertos cuyo contrato está pendiente son opcionales (`null`): la interfaz muestra entonces un
 * estado explícito de "contrato pendiente" en lugar de inventar datos (docs/pendientes.md).
 */
class Adapters(
    val dataSource: DataSource,
    val auth: AuthPort,
    val homeScope: HomeScopePort,
    val telemetryQuery: TelemetryQueryPort,
    val telemetryNotifier: TelemetryChangeNotifier,
    val alertQuery: AlertQueryPort,
    val capabilities: CapabilitiesPort?,
    val aggregates: MetricAggregatePort?,
    val floorPlans: FloorPlanPort?,
    val missions: MissionQueryPort?,
) {
    companion object {

        /** Adaptadores de Supabase. Capacidades, agregados, planos y misiones esperan su contrato (B8, B9). */
        fun supabase(config: SupabaseConfig, scope: CoroutineScope): Adapters {
            val client = SupabaseClientFactory.create(config)
            return Adapters(
                dataSource = DataSource.Supabase,
                auth = SupabaseAuthAdapter(client, scope),
                homeScope = SupabaseHomeScopeAdapter(client),
                telemetryQuery = SupabaseTelemetryQueryAdapter(client),
                telemetryNotifier = SupabaseTelemetryChangeNotifier(client),
                alertQuery = SupabaseAlertQueryAdapter(client),
                capabilities = null,
                aggregates = null,
                floorPlans = null,
                missions = null,
            )
        }

        /** Adaptadores del simulador local (HU15). Implementa todos los puertos, incluso los provisionales. */
        fun simulated(clock: Clock, scope: CoroutineScope): Adapters {
            val world = DemoWorld(clock, scope)
            return Adapters(
                dataSource = DataSource.Simulated(world),
                auth = DemoAuthAdapter(),
                homeScope = DemoHomeScopeAdapter(world),
                telemetryQuery = DemoTelemetryQueryAdapter(world),
                telemetryNotifier = DemoTelemetryChangeNotifier(world),
                alertQuery = DemoAlertQueryAdapter(world),
                capabilities = DemoCapabilitiesAdapter(world),
                aggregates = DemoMetricAggregateAdapter(world),
                floorPlans = DemoFloorPlanAdapter(world),
                missions = DemoMissionAdapter(world),
            )
        }
    }
}
