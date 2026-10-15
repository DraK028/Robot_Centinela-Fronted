package com.robotcentinela.app.identity.ports

import com.robotcentinela.app.identity.domain.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Puerto de autenticación de personas (RF01: "iniciar y cerrar sesión y recuperar acceso
 * mediante Supabase Auth").
 *
 * El método de credenciales no está fijado en la documentación. Esta versión usa correo y
 * contraseña porque RF01 pide "recuperar acceso" (ver docs/pendientes.md).
 */
interface AuthPort {

    /** Estado actual de la sesión, observable. */
    val sessionState: StateFlow<SessionState>

    /** Inicia sesión. Lanza [com.robotcentinela.app.shared.AccessDeniedException] si se rechaza. */
    suspend fun signInWithEmail(email: String, password: String)

    /** Cierra la sesión actual. */
    suspend fun signOut()

    /** Solicita al servidor el envío de instrucciones para recuperar el acceso. */
    suspend fun requestAccessRecovery(email: String)
}
