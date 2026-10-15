import java.util.Properties

// Módulo :app — adaptadores e infraestructura Android.
// Implementa los puertos de :core con supabase-kt y contiene la raíz de composición (shell).
// Todavía no hay UI: el prototipo de interfaces se descartó y se espera el del equipo de
// Ingeniería de Software (ver docs/pendientes.md).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

// Configuración local de Supabase. Se lee de local.properties, que está en .gitignore.
// Solo se admite la clave PÚBLICA (anon/publishable). Nunca service_role (expediente §7.3, RNF08).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProperty(name: String): String = localProperties.getProperty(name, "")

android {
    namespace = "com.robotcentinela.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.robotcentinela.app"
        // minSdk 26: permite java.time (UTC, zonas horarias) sin desugaring. Provisional hasta
        // que Ingeniería de Software defina los dispositivos objetivo.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SUPABASE_URL", "\"${localProperty("supabase.url")}\"")
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperty("supabase.publishableKey")}\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // En módulos Android hay que elegir explícitamente la variante JUnit 5 de kotlin-test.
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
