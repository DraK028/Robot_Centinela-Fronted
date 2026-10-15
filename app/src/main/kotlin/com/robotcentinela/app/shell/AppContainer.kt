package com.robotcentinela.app.shell

import com.robotcentinela.app.environmentalmonitoring.application.DescribeChannelStatus
import com.robotcentinela.app.environmentalmonitoring.domain.FreshnessPolicy
import com.robotcentinela.app.identity.adapters.supabase.SupabaseAuthAdapter
import com.robotcentinela.app.identity.adapters.supabase.SupabaseHomeScopeAdapter
import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.application.ListAuthorizedHomes
import com.robotcentinela.app.identity.application.RecoverAccess
import com.robotcentinela.app.identity.application.SelectHomeScope
import com.robotcentinela.app.identity.application.SignIn
import com.robotcentinela.app.identity.application.SignOut
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.identity.ports.AuthPort
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.incidents.adapters.supabase.SupabaseAlertQueryAdapter
import com.robotcentinela.app.incidents.application.QueryAlertHistory
import com.robotcentinela.app.incidents.ports.AlertQueryPort
import com.robotcentinela.app.robothealth.application.EvaluateRobotHealth
import com.robotcentinela.app.robothealth.domain.IngestConnectivityPolicy
import com.robotcentinela.app.shell.supabase.SupabaseClientFactory
import com.robotcentinela.app.shell.supabase.SupabaseConfig
import com.robotcentinela.app.telemetry.adapters.supabase.SupabaseTelemetryChangeNotifier
import com.robotcentinela.app.telemetry.adapters.supabase.SupabaseTelemetryQueryAdapter
import com.robotcentinela.app.telemetry.application.QueryTelemetryHistory
import com.robotcentinela.app.telemetry.application.SyncConfig
import com.robotcentinela.app.telemetry.application.TelemetrySynchronizer
import com.robotcentinela.app.telemetry.ports.TelemetryChangeNotifier
import com.robotcentinela.app.telemetry.ports.TelemetryQueryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Raíz de composición: el único lugar que conoce a la vez los puertos y sus implementaciones
 * (expediente §14: "composition enlaza implementaciones").
 *
 * La inyección de dependencias es manual (ADR-002). La documentación no exige ningún framework y así
 * la app no agrega dependencias innecesarias.
 *
 * Todavía no se conectan [com.robotcentinela.app.environmentalmonitoring.ports.CapabilitiesPort] ni
 * [com.robotcentinela.app.environmentalmonitoring.ports.MetricAggregatePort]: sus contratos están
 * pendientes (docs/pendientes.md).
 */
class AppContainer(
    config: SupabaseConfig,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** Ámbito de vida de la aplicación, para estados compartidos como la sesión. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val supabase = SupabaseClientFactory.create(config)

    // --- Adaptadores (implementaciones de los puertos de :core) ---
    val authPort: AuthPort = SupabaseAuthAdapter(supabase, applicationScope)
    private val homeScopePort: HomeScopePort = SupabaseHomeScopeAdapter(supabase)
    private val telemetryQuery: TelemetryQueryPort = SupabaseTelemetryQueryAdapter(supabase)
    private val telemetryNotifier: TelemetryChangeNotifier = SupabaseTelemetryChangeNotifier(supabase)
    private val alertQuery: AlertQueryPort = SupabaseAlertQueryAdapter(supabase)

    // --- Estado compartido de la sesión ---
    val currentHomeScope = CurrentHomeScope()

    // --- Políticas con sus valores iniciales documentados (ADR-004) ---
    private val freshnessPolicy = FreshnessPolicy()
    private val ingestConnectivityPolicy = IngestConnectivityPolicy()

    // --- Casos de uso ---
    val signIn = SignIn(authPort)
    val signOut = SignOut(authPort, currentHomeScope)
    val recoverAccess = RecoverAccess(authPort)
    val listAuthorizedHomes = ListAuthorizedHomes(homeScopePort)
    val selectHomeScope = SelectHomeScope(homeScopePort, currentHomeScope)
    val queryTelemetryHistory = QueryTelemetryHistory(telemetryQuery, currentHomeScope)
    val queryAlertHistory = QueryAlertHistory(alertQuery, currentHomeScope)
    val describeChannelStatus = DescribeChannelStatus(freshnessPolicy, clock)
    val evaluateRobotHealth = EvaluateRobotHealth(ingestConnectivityPolicy, freshnessPolicy, clock)

    /**
     * Crea un sincronizador para un robot del domicilio seleccionado (HU03).
     *
     * @throws com.robotcentinela.app.shared.NotInScopeException si el robot no pertenece al ámbito.
     */
    fun telemetrySynchronizer(robotId: RobotId, config: SyncConfig = SyncConfig()): TelemetrySynchronizer {
        currentHomeScope.requireRobot(robotId)
        return TelemetrySynchronizer(robotId, telemetryQuery, telemetryNotifier, config)
    }
}
