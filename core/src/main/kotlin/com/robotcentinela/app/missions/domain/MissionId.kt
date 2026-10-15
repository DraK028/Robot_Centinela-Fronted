package com.robotcentinela.app.missions.domain

import java.util.UUID

/**
 * Identificador de misión (`mission_id` del frame; `missions.id`, expediente §13.2).
 *
 * Puede faltar en un frame cuando no hay misión activa (§7.2). El resumen de misión (RF14) queda
 * pendiente hasta que backend publique su contrato (ver docs/pendientes.md).
 */
@JvmInline
value class MissionId(val value: UUID)
