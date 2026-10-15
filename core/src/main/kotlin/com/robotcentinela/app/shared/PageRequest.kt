package com.robotcentinela.app.shared

/**
 * Parámetros de paginación de una consulta de historial (RF10: "con paginación y agregación
 * explícita"). Evita descargar tablas completas (RNF05).
 */
data class PageRequest(val limit: Int, val offset: Long = 0) {

    init {
        require(limit > 0) { "El tamaño de página debe ser positivo" }
        require(offset >= 0) { "El desplazamiento no puede ser negativo" }
    }

    /** Página siguiente con el mismo tamaño. */
    fun next(): PageRequest = copy(offset = offset + limit)
}
