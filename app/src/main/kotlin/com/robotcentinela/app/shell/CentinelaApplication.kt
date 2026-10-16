package com.robotcentinela.app.shell

import android.app.Application
import com.robotcentinela.app.BuildConfig
import com.robotcentinela.app.shell.supabase.SupabaseConfig

/**
 * Punto de entrada Android. Expone el [AppContainer] a la capa de presentación.
 *
 * El origen de datos se elige con `centinela.dataSource` en `local.properties` (ADR-005):
 * - `supabase`: Supabase; si falta la configuración, la interfaz muestra el error con su causa.
 * - `demo`: simulador local rotulado como "Datos simulados" (HU15).
 * - vacío: Supabase si está configurado; si no, el simulador.
 */
class CentinelaApplication : Application() {

    /** Contenedor, o el error de configuración que impide crearlo. */
    val containerResult: Result<AppContainer> by lazy {
        when (BuildConfig.DATA_SOURCE.trim().lowercase()) {
            "demo" -> runCatching { AppContainer.simulated() }
            "supabase" -> runCatching { AppContainer.supabase(SupabaseConfig.fromBuildConfig()) }
            else -> runCatching { SupabaseConfig.fromBuildConfig() }.fold(
                onSuccess = { config -> runCatching { AppContainer.supabase(config) } },
                onFailure = { runCatching { AppContainer.simulated() } },
            )
        }
    }

    /** Contenedor de dependencias. Solo se usa cuando [containerResult] fue exitoso. */
    val container: AppContainer get() = containerResult.getOrThrow()
}
