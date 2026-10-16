package com.robotcentinela.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.robotcentinela.app.R
import com.robotcentinela.app.ui.app.uiDependencies
import com.robotcentinela.app.ui.components.AppIcon
import com.robotcentinela.app.ui.components.SectionCard
import com.robotcentinela.app.ui.components.SimulationTag
import com.robotcentinela.app.ui.theme.CentinelaShapes
import com.robotcentinela.app.ui.theme.CentinelaTheme
import com.robotcentinela.app.ui.theme.Spacing

/** Acceso del residente (HU01, RF01). */
@Composable
fun LoginScreen(justSignedOut: Boolean) {
    val deps = uiDependencies
    val vm: LoginViewModel = viewModel { LoginViewModel(deps.container, deps.preferences) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = CentinelaTheme.colors
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        // Banda superior navy (misma identidad que la cabecera de la app).
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.chrome)
                .statusBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            AppIcon(R.drawable.ic_smart_toy, size = 48.dp, tint = colors.onChrome)
            Text("Robot Centinela", style = MaterialTheme.typography.headlineSmall, color = colors.onChrome, modifier = Modifier.semantics { heading() })
            Text("Consulta del monitoreo de tu domicilio", style = MaterialTheme.typography.bodyMedium, color = colors.onChrome)
        }

        Box(Modifier.fillMaxWidth().padding(Spacing.l), contentAlignment = Alignment.TopCenter) {
            SectionCard(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                title = if (state.recoveryMode) "Recuperar acceso" else "Iniciar sesión",
                trailing = if (deps.simulated) { { SimulationTag() } } else null,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    if (justSignedOut && !state.recoveryMode) {
                        Text("Cerraste sesión. Vuelve a ingresar para consultar tu domicilio.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    if (deps.simulated) {
                        Text(
                            "Modo demostración: usa cualquier correo y una contraseña de 6 caracteres o más.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.focus,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = colors.textPrimary,
                        unfocusedLabelColor = colors.textSecondary,
                        errorBorderColor = colors.critical,
                        errorLabelColor = colors.critical,
                        errorSupportingTextColor = colors.critical,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                    )

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = vm::onEmail,
                        label = { Text("Correo electrónico") },
                        supportingText = { Text(state.emailError ?: "El correo con el que te registraron.") },
                        isError = state.emailError != null,
                        singleLine = true,
                        enabled = !state.loading,
                        leadingIcon = { AppIcon(R.drawable.ic_mail, tint = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = if (state.recoveryMode) ImeAction.Done else ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onDone = { vm.requestRecovery() }),
                        shape = CentinelaShapes.control,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!state.recoveryMode) {
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = vm::onPassword,
                            label = { Text("Contraseña") },
                            supportingText = { state.passwordError?.let { Text(it) } },
                            isError = state.passwordError != null,
                            singleLine = true,
                            enabled = !state.loading,
                            leadingIcon = { AppIcon(R.drawable.ic_lock, tint = colors.textSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    AppIcon(
                                        if (showPassword) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                        tint = colors.textSecondary,
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { vm.signIn() }),
                            shape = CentinelaShapes.control,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    state.formError?.let {
                        Row(
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        ) {
                            AppIcon(R.drawable.ic_error, tint = colors.critical)
                            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                        }
                    }
                    state.recoveryMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }

                    Button(
                        onClick = { if (state.recoveryMode) vm.requestRecovery() else vm.signIn() },
                        enabled = !state.loading,
                        shape = CentinelaShapes.control,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                        modifier = Modifier.fillMaxWidth().heightIn(min = CentinelaShapes.minTouchTarget),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                            Text(if (state.recoveryMode) "  Enviando…" else "  Verificando…")
                        } else {
                            Text(if (state.recoveryMode) "Enviar instrucciones" else "Iniciar sesión")
                        }
                    }
                    TextButton(
                        onClick = vm::toggleRecovery,
                        enabled = !state.loading,
                        modifier = Modifier.heightIn(min = CentinelaShapes.minTouchTarget),
                    ) {
                        Text(
                            if (state.recoveryMode) "Volver a iniciar sesión" else "¿Olvidaste tu contraseña?",
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
        Text(
            "Esta app es de consulta: no controla el robot. El inicio, la pausa y la parada se hacen en el robot.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding().padding(bottom = Spacing.l),
        )
    }
}
