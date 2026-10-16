package com.robotcentinela.app.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robotcentinela.app.R
import com.robotcentinela.app.ui.app.uiDependencies
import com.robotcentinela.app.ui.components.AppIcon
import com.robotcentinela.app.ui.components.EmptyState
import com.robotcentinela.app.ui.components.FilterChipRow
import com.robotcentinela.app.ui.components.FloorPlanView
import com.robotcentinela.app.ui.components.MapLegend
import com.robotcentinela.app.ui.components.ScreenTitle
import com.robotcentinela.app.ui.components.SectionCard
import com.robotcentinela.app.ui.components.StatusBadge
import com.robotcentinela.app.ui.main.FloorPlanState
import com.robotcentinela.app.ui.main.LiveUiState
import com.robotcentinela.app.ui.model.MapLayers
import com.robotcentinela.app.ui.model.OperationPresentation
import com.robotcentinela.app.ui.summary.mapSummary
import com.robotcentinela.app.ui.summary.planNote
import com.robotcentinela.app.ui.theme.CentinelaShapes
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing

private enum class Layer(val label: String) { Route("Ruta"), Samples("Muestras en estaciones"), Transit("Lecturas en tránsito") }

/** Mapa asistido 2D de la misión en curso (HU05, HU06, guía §12.5). */
@Composable
fun MapScreen(live: LiveUiState, onOpenMissions: () -> Unit) {
    val colors = CentinelaTheme.colors
    val deps = uiDependencies
    var route by rememberSaveable { mutableStateOf(true) }
    var samples by rememberSaveable { mutableStateOf(true) }
    var transit by rememberSaveable { mutableStateOf(false) }

    fun toggle(layer: Layer) {
        when (layer) {
            Layer.Route -> route = !route
            Layer.Samples -> samples = !samples
            Layer.Transit -> transit = !transit
        }
    }
    val plan = (live.floorPlan as? FloorPlanState.Available)?.plan

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val mapHeight = (maxHeight * 0.6f).coerceAtLeast(320.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            ScreenTitle("Mapa asistido 2D", live.robot?.let { "Robot ${it.publicCode} · misión en curso" }, deps.simulated)

            if (live.robotMarker == null && live.trajectory.isEmpty() && plan == null) {
                EmptyState(
                    icon = R.drawable.ic_map,
                    title = "Sin posición para mostrar",
                    message = "Los frames recientes no traen pose. Sin pose no hay posición: el robot nunca se dibuja en un " +
                        "punto supuesto.",
                )
            } else {
                SectionCard(contentPadding = Spacing.s) {
                    FloorPlanView(
                        plan = plan,
                        trajectory = live.trajectory,
                        robot = live.robotMarker,
                        layers = MapLayers(route = route, samples = samples, transit = transit),
                        modifier = Modifier.fillMaxWidth().height(mapHeight),
                        interactive = true,
                        signalLabel = live.signalLabel,
                        accessibilitySummary = mapSummary(live),
                    )
                }
                FilterChipRow(
                    options = Layer.entries,
                    selected = buildSet {
                        if (route) add(Layer.Route)
                        if (samples) add(Layer.Samples)
                        if (transit) add(Layer.Transit)
                    },
                    label = { it.label },
                    onToggle = ::toggle,
                )
                MapLegend(showSamples = samples)
            }

            SectionCard(title = "Posición") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    AppIcon(R.drawable.ic_navigation, tint = colors.textSecondary)
                    Text(
                        live.poseText?.let { "Estimada: $it" } ?: "Sin pose en el último frame",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (live.poseStale) {
                    Text(
                        "Es la posición del último reporte y está atrasada (${live.lastUpdate ?: "sin hora"}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                    )
                }
                Text(
                    "La posición es una estimación con su incertidumbre: el círculo discontinuo no es el tamaño del robot. " +
                        "La app no asigna lecturas a habitaciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                planNote(live.floorPlan)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }

            SectionCard(title = "Misión") {
                val mission = live.mission
                if (mission != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        StatusBadge(OperationPresentation.missionStatus(mission.status))
                        Text(
                            "Estaciones ${OperationPresentation.stationProgress(mission)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Text(
                        if (live.missionsSupported) "Sin misión activa en el último frame." else "Resumen de misión pendiente de contrato.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                OutlinedButton(onClick = onOpenMissions, shape = CentinelaShapes.control, modifier = Modifier.padding(top = Spacing.s)) {
                    Text("Ver misiones y comparar recorridos")
                }
            }
        }
    }
}
