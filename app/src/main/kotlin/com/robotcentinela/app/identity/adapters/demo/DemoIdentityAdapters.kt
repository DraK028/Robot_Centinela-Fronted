package com.robotcentinela.app.identity.adapters.demo

import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.identity.domain.MembershipRole
import com.robotcentinela.app.identity.domain.MembershipStatus
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.identity.domain.SessionState
import com.robotcentinela.app.identity.domain.UserId
import com.robotcentinela.app.identity.ports.AuthPort
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.shared.AccessDeniedException
import com.robotcentinela.app.shell.demo.DemoWorld
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Autenticación simulada (HU15). Acepta cualquier correo con formato válido y una contraseña de al
 * menos 6 caracteres; con otra combinación responde como un rechazo del servidor, para probar los
 * mensajes de error del formulario (§12.5).
 *
 * No guarda la sesión: al reiniciar la app se vuelve a pedir acceso.
 */
class DemoAuthAdapter : AuthPort {

    private val state = MutableStateFlow<SessionState>(SessionState.NotAuthenticated(signedOut = false))
    override val sessionState: StateFlow<SessionState> = state.asStateFlow()

    override suspend fun signInWithEmail(email: String, password: String) {
        delay(LATENCY_MS)
        if (!email.contains('@') || password.length < MIN_PASSWORD) {
            throw AccessDeniedException("inicio de sesión: credenciales rechazadas (simulado)")
        }
        state.value = SessionState.Authenticated(UserId(UUID.nameUUIDFromBytes(email.lowercase().toByteArray()).toString()))
    }

    override suspend fun signOut() {
        delay(LATENCY_MS / 2)
        state.value = SessionState.NotAuthenticated(signedOut = true)
    }

    override suspend fun requestAccessRecovery(email: String) {
        delay(LATENCY_MS)
    }

    private companion object {
        const val LATENCY_MS = 600L
        const val MIN_PASSWORD = 6
    }
}

/**
 * Membresías simuladas: un domicilio con robot y otro sin robot, para probar la selección explícita
 * de domicilio (RF02) y el estado "sin robot" (§12.5).
 */
class DemoHomeScopeAdapter(private val world: DemoWorld) : HomeScopePort {

    override suspend fun memberships(): List<HomeMembership> {
        world.requireOnline("membresías")
        delay(300)
        return listOf(
            HomeMembership(world.mainHome, MembershipRole("reader"), MembershipStatus.Active),
            HomeMembership(world.emptyHome, MembershipRole("reader"), MembershipStatus.Active),
        )
    }

    override suspend fun robotsOf(homeId: HomeId): List<Robot> {
        world.requireOnline("robots")
        return if (homeId == world.mainHome.id) listOf(world.robot) else emptyList()
    }
}
