package com.robotcentinela.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.robotcentinela.app.R
import com.robotcentinela.app.ui.model.BatteryModel
import com.robotcentinela.app.ui.model.ConnectivityItem
import com.robotcentinela.app.ui.model.MetricCardModel
import com.robotcentinela.app.ui.model.StatusLabel
import com.robotcentinela.app.ui.theme.CentinelaShapes
import com.robotcentinela.app.ui.theme.CentinelaText
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing

/**
 * Tarjeta de métrica (guía §12.5, HU02, HU04, HU20).
 *
 * - Sin valor: raya "—" con la leyenda del estado ("Sin dato", "No instalado", "Calentando"); nunca 0.
 * - Dato atrasado: borde de aviso, ícono de reloj y hora exacta de la última lectura, sin animaciones.
 * - Toda la tarjeta es un solo elemento para lectores de pantalla, con una descripción completa.
 */
@Composable
fun MetricCard(model: MetricCardModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CentinelaTheme.colors
    Surface(
        onClick = onOpen,
        modifier = modifier
            .heightIn(min = 148.dp)
            .clearAndSetSemantics {
                contentDescription = model.accessibilityText
                role = Role.Button
                onClick(label = "Ver detalle de ${model.title}") { onOpen(); true }
            },
        shape = CentinelaShapes.card,
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = BorderStroke(
            if (model.stale) CentinelaShapes.focusWidth else CentinelaShapes.borderWidth,
            if (model.stale) colors.warning else colors.border,
        ),
    ) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(model.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            model.subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 2)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(model.value ?: "—", style = CentinelaText.metricValue, maxLines = 1)
                model.unit?.let {
                    Text(it, style = CentinelaText.metricUnit, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            StatusBadge(model.status)
            if (model.stale) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AppIcon(R.drawable.ic_schedule, size = 16.dp, tint = colors.textPrimary)
                    Text(
                        "Atrasado · última lectura ${model.capturedAt ?: "sin hora"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary,
                    )
                }
            } else if (model.age != null) {
                Text(
                    listOfNotNull(model.capturedAt, model.age).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** Esqueleto de una tarjeta de métrica, para la carga inicial. */
@Composable
fun MetricCardSkeleton(modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier.heightIn(min = 148.dp)) {
        SkeletonBlock(Modifier.width(96.dp))
        SkeletonBlock(Modifier.width(72.dp).padding(top = Spacing.s), height = 32.dp)
        SkeletonBlock(Modifier.width(110.dp).padding(top = Spacing.s))
    }
}

/**
 * Barra de conectividad (§12.5): distingue robot, servidor de ingesta y esta app, y muestra "estado
 * desconocido" cuando no hay información.
 */
@Composable
fun ConnectivityBar(items: List<ConnectivityItem>, modifier: Modifier = Modifier) {
    val colors = CentinelaTheme.colors
    SectionCard(modifier = modifier, title = "Conectividad", contentPadding = Spacing.m) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics {
                        contentDescription = "${item.title}: ${item.status.text}. ${item.detail ?: ""}"
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    AppIcon(
                        when (item.title) {
                            "Robot" -> R.drawable.ic_smart_toy
                            "Ingesta" -> R.drawable.ic_router
                            else -> R.drawable.ic_smartphone
                        },
                        tint = colors.textSecondary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.labelMedium)
                        item.detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary) }
                    }
                    StatusBadge(item.status)
                }
            }
        }
    }
}

/** Panel de operación: batería con método, estaciones y última actualización (HU08, propuesta móvil). */
@Composable
fun OperationCard(
    battery: BatteryModel,
    stations: String?,
    lastUpdate: String?,
    robotState: StatusLabel?,
    modifier: Modifier = Modifier,
) {
    val colors = CentinelaTheme.colors
    SectionCard(modifier = modifier, title = "Operación", trailing = { robotState?.let { StatusBadge(it) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text("Batería", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Text(battery.primary ?: "—", style = MaterialTheme.typography.headlineSmall)
                Text(battery.secondary, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                battery.method?.let {
                    Text("Método: $it", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text("Estaciones", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Text(stations ?: "—", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (stations == null) "Resumen de misión no disponible" else "Misión en curso",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
                Text(
                    "Última actualización: ${lastUpdate ?: "sin datos"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
