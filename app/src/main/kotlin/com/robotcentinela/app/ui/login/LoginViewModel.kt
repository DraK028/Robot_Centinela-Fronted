package com.robotcentinela.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotcentinela.app.shared.AccessDeniedException
import com.robotcentinela.app.shell.AppContainer
import com.robotcentinela.app.ui.app.UiPreferences
import com.robotcentinela.app.ui.model.OperationPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del formulario de acceso (HU01, §12.5).
 *
 * Los errores se vinculan al campo que los causa; el correo se conserva tras un error; nunca se
 * bloquea el formulario sin explicar por qué.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val recoveryMode: Boolean = false,
    val recoveryMessage: String? = null,
)

class LoginViewModel(
    private val container: AppContainer,
    private val preferences: UiPreferences,
) : ViewModel() {

    private val mutable = MutableStateFlow(LoginUiState(email = preferences.lastEmail))
    val state: StateFlow<LoginUiState> = mutable.asStateFlow()

    fun onEmail(value: String) = mutable.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPassword(value: String) = mutable.update { it.copy(password = value, passwordError = null, formError = null) }

    fun toggleRecovery() = mutable.update {
        it.copy(recoveryMode = !it.recoveryMode, recoveryMessage = null, formError = null, emailError = null, passwordError = null)
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Escribe tu correo."
        !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim()) -> "Escribe un correo válido, por ejemplo nombre@dominio.com."
        else -> null
    }

    fun signIn() {
        val current = mutable.value
        if (current.loading) return
        val emailError = validateEmail(current.email)
        val passwordError = if (current.password.isEmpty()) "Escribe tu contraseña." else null
        if (emailError != null || passwordError != null) {
            mutable.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        mutable.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                container.signIn(current.email, current.password)
                preferences.lastEmail = current.email.trim()
                mutable.update { it.copy(loading = false, password = "") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccessDeniedException) {
                mutable.update {
                    it.copy(
                        loading = false,
                        password = "",
                        passwordError = "Correo o contraseña incorrectos. Revisa los datos e inténtalo de nuevo.",
                    )
                }
            } catch (e: Exception) {
                mutable.update { it.copy(loading = false, formError = OperationPresentation.errorMessage(e)) }
            }
        }
    }

    fun requestRecovery() {
        val current = mutable.value
        if (current.loading) return
        val emailError = validateEmail(current.email)
        if (emailError != null) {
            mutable.update { it.copy(emailError = emailError) }
            return
        }
        mutable.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                container.recoverAccess(current.email)
                // Mensaje neutro: no revela si el correo está registrado.
                mutable.update {
                    it.copy(
                        loading = false,
                        recoveryMessage = "Si el correo está registrado, recibirás instrucciones para recuperar el acceso.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update { it.copy(loading = false, formError = OperationPresentation.errorMessage(e)) }
            }
        }
    }
}
