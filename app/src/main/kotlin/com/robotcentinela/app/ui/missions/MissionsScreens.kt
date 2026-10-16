package com.robotcentinela.app.ui.missions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robotcentinela.app.R
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.missions.domain.MissionId
import com.robotcentinela.app.missions.domain.MissionSummary
import com.robotcentinela.app.ui.app.uiDependencies
import com.robotcentinela.app.ui.components.AppIcon
import com.robotcentinela.app.ui.components.DetailRow
import com.robotcentinela.app.ui.components.EmptyState
import com.robotcentinela.app.ui.components.ErrorState
import com.robotcentinela.app.ui.components.FloorPlanView
import com.robotcentinela.app.ui.components.MapLegend
import com.robotcentinela.app.ui.components.PagerControls
import com.robotcentinela.app.ui.components.ScreenTitle
import com.robotcentinela.app.ui.components.SectionCard
import com.robotcentinela.app.ui.components.SkeletonBlock
import com.robotcentinela.app.ui.components.StatusBadge
import com.robotcentinela.app.ui.main.FloorPlanState
import com.robotcentinela.app.ui.model.Formats
import com.robotcentinela.app.ui.model.MapLayers
import com.robotcentinela.app.ui.model.OperationPresentation
import com.robotcentinela.app.ui.summary.planNote
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

/** Lista de misiones del robot (RF14). */
@Composable
fun MissionsScreen(robot: Robot, zone: ZoneId, currentMissionId: MissionId?, onOpenMission: (String) -> Unit) {
    val deps = uiDependencies
    val vm: MissionsViewModel = viewModel { MissionsViewModel(deps.container, robot) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = CentinelaTheme.colors

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        ScreenTitle("Misiones", "Recorridos del robot ${robot.publicCode}", deps.simulated)
        if (!state.supported) {
            EmptyState(
                icon = R.drawable.ic_route,
                title = "Resumen de misiones pendiente",
                message = "El contrato de misiones (estaciones previstas, visitadas y omitidas, consumo) todavía no está " +
                    "publicado por backend. " +
                    (currentMissionId?.let { "El último frame pertenece a una misión en curso." } ?: "El último frame no indica misión."),
            )
        } else {
            SectionCard {
                val error = state.error
                when {
                    state.loading && state.missions.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        repeat(4) { SkeletonBlock(Modifier.fillMaxWidth(), height = 40.dp) }
                    }
                    error != null -> ErrorState(OperationPresentation.errorMessage(error), onRetry = vm::retry)
                    state.missions.isEmpty() -> EmptyState(R.drawable.ic_route, "Sin misiones", "El robot todavía no registra misiones.")
                    else -> Column {
                        state.missions.forEachIndexed { index, mission ->
                            if (index > 0) HorizontalDivider(color = colors.border)
                            MissionRow(mission, zone, onClick = { onOpenMission(mission.id.value.toString()) })
                        }
                    }
                }
                PagerControls(
                    page = state.page,
                    hasPrevious = state.page > 0 && !state.loading,
                    hasNext = state.hasNext && !state.loading,
                    onPrevious = vm::previous,
                    onNext = vm::next,
                    modifier = Modifier.padding(top = Spacing.m),
                )
            }
        }
    }
}

@Composable
private fun MissionRow(mission: MissionSummary, zone: ZoneId, onClick: () -> Unit) {
    val colors = CentinelaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = "Ver detalle de la misión", role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(Modifier.weight(1f)) {
            Text(Formats.dateTime(mission.startedAt, zone), style = MaterialTheme.typography.titleSmall)
            Text(
                "Estaciones ${OperationPresentation.stationProgress(mission)}" +
                    (if (mission.omittedStations > 0) " · ${mission.omittedStations} omitida(s)" else "") +
                    " · ${OperationPresentation.missionDuration(mission, Instant.now())}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        StatusBadge(OperationPresentation.missionStatus(mission.status))
        AppIcon(R.drawable.ic_chevron_right, tint = colors.textSecondary)
    }
}

/** Detalle de misión (HU05, HU06, HU08). */
@Composable
fun MissionDetailScreen(robot: Robot, zone: ZoneId, missionId: String, onBack: () -> Unit) {
    val deps = uiDependencies
    val vm: MissionDetailViewModel = viewModel { MissionDetailViewModel(deps.container, robot, zone, missionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = CentinelaTheme.colors

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                AppIcon(R.drawable.ic_arrow_back, contentDescription = "Volver a misiones", size = 24.dp, tint = colors.textPrimary)
            }
            Text("Detalle de misión", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        val mission = state.mission
        val error = state.error
        when {
            state.loading -> SkeletonBlock(Modifier.fillMaxWidth(), height = 200.dp)
            state.notFound -> EmptyState(R.drawable.ic_route, "Misión no encontrada", "La misión no existe o no pertenece a este domicilio.")
            mission == null && error != null -> ErrorState(OperationPresentation.errorMessage(error), onRetry = vm::retry)
            mission != null -> {
                SectionCard(title = "Resumen", trailing = { StatusBadge(OperationPresentation.missionStatus(mission.status)) }) {
                    DetailRow("Inicio", Formats.dateTime(mission.startedAt, zone))
                    DetailRow("Fin", mission.endedAt?.let { Formats.dateTime(it, zone) } ?: "En curso")
                    DetailRow("Duración", OperationPresentation.missionDuration(mission, Instant.now()))
                    DetailRow("Estaciones", "${OperationPresentation.stationProgress(mission)} visitadas")
                    DetailRow(
                        "Versión de mapa",
                        mission.mapVersionId?.value?.toString()?.takeLast(8)?.let { "…$it (la usada en esta misión)" } ?: "Sin versión registrada",
                    )
                }

                SectionCard(title = "Trayectoria") {
                    val plan = (state.floorPlan as? FloorPlanState.Available)?.plan
                    if (state.trajectory.isEmpty()) {
                        Text("Sin poses registradas para esta misión.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        FloorPlanView(
                            plan = plan,
                            trajectory = state.trajectory,
                            robot = null,
                            layers = MapLayers(route = true, samples = true),
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            interactive = true,
                            signalLabel = "Señal relativa · MQ6 (ADC, sin calibrar)",
                            accessibilitySummary = "Trayectoria de la misión con ${state.trajectory.size} puntos" +
                                (if (plan != null) " sobre su versión de mapa." else " sobre cuadrícula métrica."),
                        )
                        MapLegend(showSamples = true, modifier = Modifier.padding(top = Spacing.s))
                    }
                    planNote(state.floorPlan)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(top = Spacing.s))
                    }
                    if (state.trajectoryTruncated) {
                        Text("Se muestran las primeras 1000 poses de la misión.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                SectionCard(title = "Estaciones") {
                    mission.stations.forEachIndexed { index, visit ->
                        if (index > 0) HorizontalDivider(color = colors.border)
                        val (label, detail) = OperationPresentation.stationOutcome(visit.outcome, zone)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                        ) {
                            AppIcon(R.drawable.ic_flag, tint = colors.textSecondary)
                            Column(Modifier.weight(1f)) {
                                Text("${visit.stationId.value} · ${visit.name}", style = MaterialTheme.typography.titleSmall)
                                detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary) }
                            }
                            StatusBadge(label)
                        }
                    }
                }

                SectionCard(title = "Energía") {
                    val energy = mission.energy
                    if (energy == null) {
                        Text("Sin datos de consumo.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    } else {
                        DetailRow("Voltaje inicial", energy.startVoltageV?.let { "${Formats.number(it, 2)} V" } ?: "Sin dato")
                        DetailRow("Voltaje final", energy.endVoltageV?.let { "${Formats.number(it, 2)} V" } ?: "Sin dato")
                        DetailRow("Consumo", energy.consumedWh?.let { "${Formats.number(it, 2)} Wh" } ?: "Sin dato")
                        DetailRow("Método", OperationPresentation.methodLabel(energy.method))
                        Text(
                            "El método indica si el consumo se midió o se estimó; una estimación no sustenta por sí sola un ensayo de autonomía.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
