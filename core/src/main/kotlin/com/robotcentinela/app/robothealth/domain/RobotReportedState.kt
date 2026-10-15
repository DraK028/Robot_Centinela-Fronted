package com.robotcentinela.app.robothealth.domain

/**
 * Estado del robot tal como lo reporta el dispositivo (`robot_state`, p. ej. `moving` o `sampling`).
 *
 * El catálogo canónico de estados y su correspondencia con los estados legacy de E01 no están
 * cerrados (RF19), así que se conserva el texto original, sin inferir posición ni mapa
 * (RF19: "PATRULLANDO legacy no implica que existe mapa").
 */
@JvmInline
value class RobotReportedState(val raw: String)
