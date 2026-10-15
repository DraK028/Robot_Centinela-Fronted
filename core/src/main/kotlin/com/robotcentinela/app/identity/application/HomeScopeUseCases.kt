package com.robotcentinela.app.identity.application

import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.identity.domain.HomeScope
import com.robotcentinela.app.identity.domain.MembershipStatus
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.shared.NotInScopeException

/**
 * Caso de uso: listar los domicilios donde la persona tiene membresía activa (RF02: "El usuario ve
 * únicamente domicilios y robots para los que tiene membresía activa").
 */
class ListAuthorizedHomes(private val port: HomeScopePort) {

    suspend operator fun invoke(): List<HomeMembership> =
        port.memberships().filter { it.status == MembershipStatus.Active }
}

/**
 * Caso de uso: seleccionar explícitamente el domicilio que se va a consultar (RF02, HU01).
 *
 * Solo admite domicilios con membresía activa. Al cambiar de domicilio se reemplaza el ámbito
 * completo, así que no se conservan robots del domicilio anterior.
 */
class SelectHomeScope(
    private val port: HomeScopePort,
    private val currentScope: CurrentHomeScope,
) {

    /** @throws NotInScopeException si no existe una membresía activa para [homeId]. */
    suspend operator fun invoke(homeId: HomeId): HomeScope {
        val membership = port.memberships().firstOrNull {
            it.home.id == homeId && it.status == MembershipStatus.Active
        } ?: throw NotInScopeException("No hay membresía activa para el domicilio solicitado")

        val robots = port.robotsOf(homeId).filter { it.homeId == homeId }
        return HomeScope(membership.home, robots).also(currentScope::set)
    }
}
