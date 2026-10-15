package com.robotcentinela.app.homemapping.domain

import java.util.UUID

/**
 * Versión de mapa usada al capturar un frame (`map_version_id`; `map_versions.id`, expediente §13.2).
 *
 * Una misión conserva la versión exacta de su mapa aunque después exista otra (HU05, RB13). La
 * geometría del mapa (habitaciones, estaciones, rutas) queda pendiente de contrato.
 */
@JvmInline
value class MapVersionId(val value: UUID)
