package com.robotcentinela.app.identity.application

import com.robotcentinela.app.identity.domain.HomeScope
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.shared.NotInScopeException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda el ámbito de domicilio seleccionado durante la sesión (RF02: "cambia de ámbito
 * explícitamente").
 *
 * En esta versión vive solo en memoria; al cerrar sesión se limpia.
 */
class CurrentHomeScope {

    private val current = MutableStateFlow<HomeScope?>(null)

    /** Ámbito seleccionado, o `null` si todavía no se ha elegido. */
    val scope: StateFlow<HomeScope?> = current.asStateFlow()

    internal fun set(scope: HomeScope) {
        current.value = scope
    }

    internal fun clear() {
        current.value = null
    }

    /**
     * Devuelve el ámbito seleccionado si contiene [robotId].
     *
     * @throws NotInScopeException si no hay ámbito o el robot pertenece a otro domicilio
     * (HU01 CDA 3, HU07 CDA 2).
     */
    fun requireRobot(robotId: RobotId): HomeScope {
        val scope = current.value ?: throw NotInScopeException("No hay un domicilio seleccionado")
        if (!scope.contains(robotId)) {
            throw NotInScopeException("El robot no pertenece al domicilio seleccionado")
        }
        return scope
    }
}
