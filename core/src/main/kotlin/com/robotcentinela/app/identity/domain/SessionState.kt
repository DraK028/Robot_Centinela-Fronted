package com.robotcentinela.app.identity.domain

/**
 * Estado de la sesión de la persona usuaria (RF01, HU01 CDA 1).
 *
 * La app no expone datos antes de validar la sesión. La validez real la decide Supabase Auth.
 */
sealed interface SessionState {

    /** Se está recuperando una sesión guardada; todavía no se sabe si es válida. */
    data object Initializing : SessionState

    /** Sesión válida. */
    data class Authenticated(val userId: UserId) : SessionState

    /**
     * No hay sesión: nunca se inició, venció sin poder renovarse o se cerró.
     *
     * @property signedOut `true` si la persona cerró sesión explícitamente.
     */
    data class NotAuthenticated(val signedOut: Boolean) : SessionState

    /**
     * Falló la renovación de la sesión (por ejemplo, sin red). La sesión puede estar vencida;
     * las consultas pueden ser rechazadas hasta que se renueve o se vuelva a autenticar.
     */
    data object RefreshFailed : SessionState
}
