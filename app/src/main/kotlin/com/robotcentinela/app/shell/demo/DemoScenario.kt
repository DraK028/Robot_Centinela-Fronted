package com.robotcentinela.app.shell.demo

/**
 * Escenarios del simulador de datos (HU15, RF35, guía de interfaces §12.6: "datos válidos completos,
 * datos ausentes o nulos, y escenarios de error").
 *
 * Los datos simulados siempre se rotulan como tales en la interfaz (HU15 CDA 2, RB29) y nunca generan
 * notificaciones hacia residentes reales (HU15 CDA 3): el simulador no tiene ningún canal de salida.
 */
enum class DemoScenario(val label: String, val description: String) {

    Complete(
        label = "Datos completos",
        description = "Frames cada 2 s con calidades válidas y sin calibrar.",
    ),

    Degraded(
        label = "Sensores ausentes o en fallo",
        description = "Calentamiento, fallo, fuera de rango, valores nulos y batería sin dato.",
    ),

    IngestInterrupted(
        label = "Ingesta interrumpida",
        description = "El robot deja de publicar: los canales pasan a atrasados y la ingesta a interrumpida.",
    ),

    ClientOffline(
        label = "Teléfono sin conexión",
        description = "Las consultas fallan; se muestra la última captura con su fecha.",
    ),
}
