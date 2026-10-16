package com.robotcentinela.app.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotcentinela.app.identity.domain.HomeScope
import com.robotcentinela.app.identity.domain.SessionState
import com.robotcentinela.app.shell.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Qué parte de la app se muestra, según la sesión y el domicilio seleccionado (HU01, RF02). */
sealed interface RootState {

    /** Se recupera la sesión guardada; no se muestra ningún dato todavía (HU01 CDA 1). */
    data object Loading : RootState

    /** Sin sesión. [signedOut] = la persona cerró sesión explícitamente. */
    data class SignedOut(val signedOut: Boolean) : RootState

    /** Con sesión, falta elegir domicilio de forma explícita (RF02). */
    data object ChoosingHome : RootState

    /**
     * Sesión y domicilio listos.
     *
     * @property sessionWarning la renovación de la sesión falló; las consultas pueden rechazarse.
     */
    data class Ready(val scope: HomeScope, val sessionWarning: Boolean) : RootState
}

class RootViewModel(private val container: AppContainer) : ViewModel() {

    private val choosingHome = MutableStateFlow(false)

    val state: StateFlow<RootState> = combine(
        container.authPort.sessionState,
        container.currentHomeScope.scope,
        choosingHome,
    ) { session, scope, choosing ->
        when (session) {
            SessionState.Initializing -> RootState.Loading
            is SessionState.NotAuthenticated -> RootState.SignedOut(session.signedOut)
            is SessionState.Authenticated ->
                if (scope == null || choosing) RootState.ChoosingHome else RootState.Ready(scope, sessionWarning = false)
            SessionState.RefreshFailed ->
                if (scope == null || choosing) RootState.ChoosingHome else RootState.Ready(scope, sessionWarning = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RootState.Loading)

    /** Vuelve al selector de domicilio sin cerrar sesión (§12.5: salida ante errores de permisos). */
    fun changeHome() {
        choosingHome.value = true
    }

    fun homeChosen() {
        choosingHome.value = false
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { container.signOut() }
            choosingHome.value = false
        }
    }
}
