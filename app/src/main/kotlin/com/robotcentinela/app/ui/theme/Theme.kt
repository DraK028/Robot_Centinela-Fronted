package com.robotcentinela.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/** Escala de espacios en múltiplos de 4 (§12.3). */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Geometría (§12.3): tarjetas 12 dp, botones e insumos 8 dp, objetivos táctiles de 44 dp o más. */
object CentinelaShapes {
    val card = RoundedCornerShape(12.dp)
    val control = RoundedCornerShape(8.dp)
    val pill = RoundedCornerShape(50)
    val minTouchTarget = 48.dp
    val borderWidth = 1.dp
    val focusWidth = 2.dp
}

private fun CentinelaColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = if (isDark) surfaceMuted else statusNeutral,
        onPrimaryContainer = textPrimary,
        secondary = secondary,
        onSecondary = if (isDark) background else textPrimary,
        secondaryContainer = if (isDark) border else statusNeutral,
        onSecondaryContainer = textPrimary,
        tertiary = warning,
        onTertiary = onWarning,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        inverseSurface = textPrimary,
        inverseOnSurface = background,
        outline = border,
        outlineVariant = border,
        error = critical,
        onError = onCritical,
        errorContainer = critical,
        onErrorContainer = onCritical,
    )
}

@Composable
fun CentinelaTheme(
    preference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val colors = if (dark) DarkCentinelaColors else LightCentinelaColors
    CompositionLocalProvider(LocalCentinelaColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = CentinelaTypography,
            shapes = Shapes(
                extraSmall = CentinelaShapes.control,
                small = CentinelaShapes.control,
                medium = CentinelaShapes.card,
                large = CentinelaShapes.card,
                extraLarge = CentinelaShapes.card,
            ),
            content = content,
        )
    }
}

/** Acceso a los tokens: `CentinelaTheme.colors.operational`. */
object CentinelaTheme {
    val colors: CentinelaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCentinelaColors.current
}
