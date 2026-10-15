package com.robotcentinela.app.shell

import android.app.Application
import com.robotcentinela.app.shell.supabase.SupabaseConfig

/**
 * Punto de entrada Android. Expone el [AppContainer] a la futura capa de presentación.
 *
 * El contenedor se crea de forma diferida: sin `local.properties` configurado, la app arranca y el
 * error de configuración aparece con un mensaje claro la primera vez que se usa el backend.
 */
class CentinelaApplication : Application() {

    val container: AppContainer by lazy { AppContainer(SupabaseConfig.fromBuildConfig()) }
}
