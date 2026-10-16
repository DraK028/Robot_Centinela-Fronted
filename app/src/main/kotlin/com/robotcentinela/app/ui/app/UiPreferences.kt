package com.robotcentinela.app.ui.app

import android.content.Context
import com.robotcentinela.app.ui.theme.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias locales de la interfaz (solo en este teléfono).
 *
 * - Tema elegido explícitamente (HU12 CDA 1).
 * - Último correo con el que se inició sesión, para conservarlo en el formulario (§12.5) y saludar.
 *
 * No se guardan contraseñas, tokens ni datos del domicilio.
 */
class UiPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("centinela_ui", Context.MODE_PRIVATE)

    private val themeState = MutableStateFlow(
        runCatching { ThemePreference.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemePreference.System),
    )
    val theme: StateFlow<ThemePreference> = themeState.asStateFlow()

    fun setTheme(value: ThemePreference) {
        prefs.edit().putString(KEY_THEME, value.name).apply()
        themeState.value = value
    }

    var lastEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_EMAIL, value).apply()
        }

    /** Nombre para el saludo, derivado de la parte local del correo ("daniel.morton" -> "Daniel Morton"). */
    fun displayName(): String? = lastEmail.substringBefore('@').takeIf { it.isNotBlank() }
        ?.split('.', '_', '-')
        ?.filter { it.isNotBlank() }
        ?.joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_EMAIL = "last_email"
    }
}
