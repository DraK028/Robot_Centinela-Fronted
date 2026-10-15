package com.robotcentinela.app.identity.adapters.supabase

import com.robotcentinela.app.identity.domain.SessionState
import com.robotcentinela.app.identity.domain.UserId
import com.robotcentinela.app.identity.ports.AuthPort
import com.robotcentinela.app.shell.supabase.supabaseCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Adaptador de [AuthPort] con Supabase Auth (RF01).
 *
 * La sesión la guarda y renueva el SDK. Este adaptador solo traduce su estado al dominio.
 *
 * @param scope ámbito de vida de la aplicación, donde se mantiene el estado de sesión.
 */
class SupabaseAuthAdapter(
    private val supabase: SupabaseClient,
    scope: CoroutineScope,
) : AuthPort {

    override val sessionState: StateFlow<SessionState> = supabase.auth.sessionStatus
        .map(::toDomain)
        .stateIn(scope, SharingStarted.Eagerly, SessionState.Initializing)

    override suspend fun signInWithEmail(email: String, password: String) = supabaseCall("inicio de sesión") {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() = supabaseCall("cierre de sesión") {
        supabase.auth.signOut()
    }

    override suspend fun requestAccessRecovery(email: String) = supabaseCall("recuperación de acceso") {
        supabase.auth.resetPasswordForEmail(email)
    }

    private fun toDomain(status: SessionStatus): SessionState = when (status) {
        is SessionStatus.Authenticated -> status.session.user?.id
            ?.let { SessionState.Authenticated(UserId(it)) }
            ?: SessionState.NotAuthenticated(signedOut = false)
        is SessionStatus.NotAuthenticated -> SessionState.NotAuthenticated(signedOut = status.isSignOut)
        is SessionStatus.RefreshFailure -> SessionState.RefreshFailed
        SessionStatus.Initializing -> SessionState.Initializing
    }
}
