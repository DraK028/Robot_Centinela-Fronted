package com.robotcentinela.app.ui.model

/**
 * Tono de un estado (guía §12.1): verde = lectura válida, azul grisáceo = no instalado o sin
 * calibración, naranja = advertencia, rojo = crítico.
 *
 * El color nunca va solo: todo estado lleva texto explícito e ícono (§12.1, accesibilidad). Un tono
 * operativo no implica ausencia de gases (RB09).
 */
enum class StatusTone { Operational, Neutral, Warning, Critical }

/** Ícono de un estado. La capa Compose lo traduce a un recurso de la familia Material Symbols. */
enum class StatusIcon {
    Valid,
    Info,
    Uncalibrated,
    NotInstalled,
    Warming,
    Fault,
    OutOfRange,
    NoData,
    Unknown,
    Stale,
    Live,
    Syncing,
    Offline,
    Error,
    Paused,
}

/** Etiqueta de estado: texto, tono e ícono. */
data class StatusLabel(val text: String, val tone: StatusTone, val icon: StatusIcon)
