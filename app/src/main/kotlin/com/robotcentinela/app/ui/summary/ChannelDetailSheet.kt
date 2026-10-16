package com.robotcentinela.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.robotcentinela.app.environmentalmonitoring.application.ChannelStatus
import com.robotcentinela.app.environmentalmonitoring.domain.DataFreshness
import com.robotcentinela.app.ui.components.DetailRow
import com.robotcentinela.app.ui.components.StatusBadge
import com.robotcentinela.app.ui.model.ChannelPresentation
import com.robotcentinela.app.ui.model.Formats
import com.robotcentinela.app.ui.theme.CentinelaText
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing
import java.time.ZoneId

/**
 * Detalle de un canal: qué se midió, cuándo y con qué calidad (HU02, HU04, HU09, HU20).
 *
 * No muestra identificadores técnicos (frame, esquema, claves): quedan para diagnóstico avanzado (§12.6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailSheet(status: ChannelStatus, zone: ZoneId, onDismiss: () -> Unit) {
    val colors = CentinelaTheme.colors
    val card = ChannelPresentation.card(status, zone)
    val stamp = status.sample?.stamp
    val capability = status.capability

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(card.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            card.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(card.value ?: "—", style = CentinelaText.metricValue)
                card.unit?.let { Text(it, style = CentinelaText.metricUnit, color = colors.textSecondary) }
            }
            StatusBadge(card.status)
            Text(ChannelPresentation.explanation(status.interpretation), style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(color = colors.border)
            Text("Tiempo", style = MaterialTheme.typography.titleSmall)
            if (stamp == null) {
                DetailRow("Captura", "Este canal no ha reportado")
            } else {
                DetailRow(
                    "Captura",
                    if (stamp.timeIsApproximate) {
                        "Hora de captura no confiable (reloj del robot sin sincronizar)"
                    } else {
                        Formats.dateTimeSeconds(stamp.reliableCapturedAt!!, zone)
                    },
                )
                DetailRow("Recepción en servidor", Formats.dateTimeSeconds(stamp.receivedAt, zone))
                if (stamp.timeIsApproximate) {
                    Text(
                        "La edad del dato se calcula con la hora de recepción como aproximación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            when (val freshness = status.freshness) {
                is DataFreshness.Fresh -> DetailRow("Frescura", "Al día · ${Formats.age(freshness.age)}")
                is DataFreshness.Stale -> DetailRow(
                    "Frescura",
                    "Atrasado · ${Formats.age(freshness.age)} (límite ${Formats.duration(freshness.limit)})",
                )
                null -> DetailRow("Frescura", "Sin dato")
            }

            HorizontalDivider(color = colors.border)
            Text("Canal", style = MaterialTheme.typography.titleSmall)
            if (capability == null) {
                Text(
                    "El catálogo de capacidades del robot no está disponible: la unidad y el gas objetivo de este canal " +
                        "están pendientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            } else {
                DetailRow("Unidad registrada", ChannelPresentation.unitLabel(capability.unitCode) ?: "Sin unidad (valor lógico)")
                capability.gasTarget?.let { DetailRow("Gas objetivo", ChannelPresentation.title(status.channelKey, capability)) }
                capability.expectedPeriod?.let { DetailRow("Periodo esperado", Formats.duration(it)) }
                DetailRow("Estado en el registro", if (capability.enabled) "Habilitado" else "No habilitado")
            }
            DetailRow(
                "Calibración",
                if (status.sample?.reading?.calibrationId != null) {
                    "Calibración registrada (fecha, responsable y vigencia pendientes de contrato)"
                } else {
                    "Sin calibración aplicada a esta lectura"
                },
            )
            if (capability?.gasTarget != null) {
                Text(
                    "Registrar un gas objetivo no certifica que el sensor mida ese gas de forma selectiva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
