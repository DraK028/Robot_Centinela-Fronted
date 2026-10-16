package com.robotcentinela.app.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robotcentinela.app.R
import com.robotcentinela.app.shell.AppContainer
import com.robotcentinela.app.ui.components.AppIcon
import com.robotcentinela.app.ui.homes.HomeSelectionScreen
import com.robotcentinela.app.ui.login.LoginScreen
import com.robotcentinela.app.ui.main.MainScreen
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing

/** Raíz de la interfaz: tema, dependencias y la pantalla que corresponde a la sesión. */
@Composable
fun CentinelaApp(containerResult: Result<AppContainer>, preferences: UiPreferences) {
    val theme by preferences.theme.collectAsStateWithLifecycle()
    CentinelaTheme(preference = theme) {
        val container = containerResult.getOrNull()
        if (container == null) {
            ConfigurationError(containerResult.exceptionOrNull()?.message ?: "Configuración incompleta")
        } else {
            AppContent(container, preferences)
        }
    }
}

@Composable
private fun AppContent(container: AppContainer, preferences: UiPreferences) {
    val dependencies = remember(container) { UiDependencies(container, preferences) }
    CompositionLocalProvider(LocalUiDependencies provides dependencies) {
        val root: RootViewModel = viewModel { RootViewModel(container) }
        val state by root.state.collectAsStateWithLifecycle()
        when (val s = state) {
            RootState.Loading -> Splash()
            is RootState.SignedOut -> LoginScreen(justSignedOut = s.signedOut)
            RootState.ChoosingHome -> HomeSelectionScreen(
                onChosen = root::homeChosen,
                onSignOut = root::signOut,
            )
            is RootState.Ready -> MainScreen(
                scope = s.scope,
                sessionWarning = s.sessionWarning,
                onChangeHome = root::changeHome,
                onSignOut = root::signOut,
            )
        }
    }
}

@Composable
private fun Splash() {
    val colors = CentinelaTheme.colors
    Box(Modifier.fillMaxSize().background(colors.chrome), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            AppIcon(R.drawable.ic_smart_toy, size = 56.dp, tint = colors.onChrome)
            Text("Robot Centinela", style = MaterialTheme.typography.headlineSmall, color = colors.onChrome)
            CircularProgressIndicator(color = colors.secondary)
            Text("Verificando sesión…", style = MaterialTheme.typography.bodySmall, color = colors.onChrome)
        }
    }
}

/** La configuración de Supabase es inválida (por ejemplo, clave privilegiada o URL sin HTTPS, RNF08). */
@Composable
private fun ConfigurationError(message: String) {
    val colors = CentinelaTheme.colors
    Column(
        Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
    ) {
        AppIcon(R.drawable.ic_error, size = 40.dp, tint = colors.critical)
        Text("No se pudo configurar la conexión", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        Text(
            "Revisa supabase.url y supabase.publishableKey en local.properties, o usa " +
                "centinela.dataSource=demo para abrir la app con datos simulados.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}
