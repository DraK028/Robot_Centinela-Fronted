package com.robotcentinela.app.identity.application

import com.robotcentinela.app.identity.ports.AuthPort

/** Caso de uso: iniciar sesión (HU01, RF01). */
class SignIn(private val auth: AuthPort) {

    suspend operator fun invoke(email: String, password: String) {
        require(email.isNotBlank()) { "El correo es obligatorio" }
        require(password.isNotEmpty()) { "La contraseña es obligatoria" }
        auth.signInWithEmail(email.trim(), password)
    }
}

/**
 * Caso de uso: cerrar sesión (HU01, RF01).
 *
 * Además de cerrar la sesión remota, olvida el ámbito seleccionado para no mostrar datos de un
 * domicilio sin una sesión válida.
 */
class SignOut(private val auth: AuthPort, private val currentScope: CurrentHomeScope) {

    suspend operator fun invoke() {
        try {
            auth.signOut()
        } finally {
            currentScope.clear()
        }
    }
}

/** Caso de uso: solicitar recuperación de acceso (RF01). */
class RecoverAccess(private val auth: AuthPort) {

    suspend operator fun invoke(email: String) {
        require(email.isNotBlank()) { "El correo es obligatorio" }
        auth.requestAccessRecovery(email.trim())
    }
}
