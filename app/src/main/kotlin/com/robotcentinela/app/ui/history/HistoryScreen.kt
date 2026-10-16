package com.robotcentinela.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robotcentinela.app.R
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.ui.app.uiDependencies
import com.robotcentinela.app.ui.components.DetailRow
import com.robotcentinela.app.ui.components.EmptyState
import com.robotcentinela.app.ui.components.ErrorState
import com.robotcentinela.app.ui.components.FilterChipRow
import com.robotcentinela.app.ui.components.LineChart
import com.robotcentinela.app.ui.components.PagerControls
import com.robotcentinela.app.ui.components.RangeSelector
import com.robotcentinela.app.ui.components.RefreshingNote
import com.robotcentinela.app.ui.components.ScreenTitle
import com.robotcentinela.app.ui.components.SectionCard
import com.robotcentinela.app.ui.components.SkeletonBlock
import com.robotcentinela.app.ui.components.StatusBadge
import com.robotcentinela.app.ui.model.Formats
import com.robotcentinela.app.ui.model.OperationPresentation
import com.robotcentinela.app.ui.model.RangePreset
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

/** Historial por fechas, con gráfica y su alternativa en tabla (HU07, guía §12.5). */
@Composable
fun HistoryScreen(robot: Robot, zone: ZoneId, onChangeHome: () -> Unit) {
    val deps = uiDependencies
    val vm: HistoryViewModel = viewModel { HistoryViewModel(deps.container, robot, zone) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = CentinelaTheme.colors

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        ScreenTitle("Historial", "Fechas en la zona del domicilio (${zone.id})", deps.simulated)

        SectionCard(title = "Consulta") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                RangeSelector(
                    range = state.range,
                    today = state.today,
                    onChange = vm::setRange,
                    presets = listOf(RangePreset.Today, RangePreset.Last7Days, RangePreset.Last30Days, RangePreset.Custom),
                    label = "Periodo:",
                )
                Text("Canal", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                FilterChipRow(
                    options = state.channels,
                    selected = setOf(state.channel),
                    label = { it.label },
                    onToggle = vm::setChannel,
                )
                Text("Vista", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                FilterChipRow(
                    options = HistoryMode.entries,
                    selected = setOf(state.mode),
                    label = { it.label },
                    onToggle = vm::setMode,
                )
            }
        }

        state.stats?.let { stats ->
            SectionCard(title = "Estadísticas del periodo") {
                DetailRow("Mínimo", stats.min)
                DetailRow("Máximo", stats.max)
                DetailRow("Promedio", stats.avg)
                DetailRow("Muestras", "${stats.valid} válidas · ${stats.invalid} sin valor o no válidas")
                Text(stats.source, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }

        when (state.mode) {
            HistoryMode.Chart -> SectionCard(
                title = state.channel.label,
                trailing = { if (state.chartLoading && state.chart.isNotEmpty()) RefreshingNote() },
            ) {
                val error = state.chartError
                when {
                    state.chartLoading && state.chart.isEmpty() -> SkeletonBlock(Modifier.fillMaxWidth(), height = 220.dp)
                    error != null -> ErrorState(
                        vm.errorText(error),
                        onRetry = vm::retry,
                        secondaryLabel = if (OperationPresentation.isPermissionError(error)) "Elegir domicilio" else null,
                        onSecondary = if (OperationPresentation.isPermissionError(error)) onChangeHome else null,
                    )
                    state.chart.isEmpty() -> EmptyState(R.drawable.ic_show_chart, "Sin datos", "No hay muestras de este canal en el periodo.")
                    else -> {
                        val multiDay = state.range.days > 1
                        LineChart(
                            points = state.chart,
                            unit = state.unit,
                            decimals = state.decimals,
                            xLabel = { ms ->
                                val instant = Instant.ofEpochMilli(ms)
                                if (multiDay) Formats.shortDate(instant.atZone(zone).toLocalDate()) + " " + Formats.shortTime(instant, zone) else Formats.shortTime(instant, zone)
                            },
                            selectedIndex = state.selectedIndex,
                            onSelect = vm::select,
                            accessibilitySummary = "Gráfica de ${state.channel.label} en ${state.range.label}. " +
                                (state.stats?.let { "Mínimo ${it.min}, máximo ${it.max}, promedio ${it.avg}. " } ?: "") +
                                "Toca la gráfica para ver un punto, o usa la vista de tabla.",
                        )
                        state.chartNote?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary) }
                        if (state.selection.isNotEmpty()) {
                            Column(
                                Modifier.padding(top = Spacing.s).semantics { liveRegion = LiveRegionMode.Polite },
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                HorizontalDivider(color = colors.border)
                                state.selection.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
            HistoryMode.Table -> SectionCard(
                title = "Muestras de ${state.channel.label}",
                trailing = { if (state.tableLoading && state.rows.isNotEmpty()) RefreshingNote() },
            ) {
                val error = state.tableError
                when {
                    state.tableLoading && state.rows.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        repeat(6) { SkeletonBlock(Modifier.fillMaxWidth()) }
                    }
                    error != null -> ErrorState(vm.errorText(error), onRetry = vm::retry)
                    state.rows.isEmpty() -> EmptyState(R.drawable.ic_table_rows, "Sin muestras", "No hay frames en el periodo.")
                    else -> Column {
                        state.rows.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(color = colors.border)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = Spacing.s).semantics(mergeDescendants = true) {},
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.time, style = MaterialTheme.typography.labelMedium)
                                    Text(row.position, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(row.value, style = MaterialTheme.typography.titleSmall)
                                        StatusBadge(row.quality)
                                    }
                                }
                            }
                        }
                    }
                }
                PagerControls(
                    page = state.page,
                    hasPrevious = state.page > 0 && !state.tableLoading,
                    hasNext = state.hasNext && !state.tableLoading,
                    onPrevious = vm::previousPage,
                    onNext = vm::nextPage,
                    modifier = Modifier.padding(top = Spacing.m),
                )
            }
        }
        Text(
            "El historial muestra las unidades tal como se registraron y conserva los huecos: un periodo sin datos no se " +
                "rellena ni se interpreta como cero.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}
