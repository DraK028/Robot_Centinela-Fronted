// Módulo :core — el hexágono.
// Contiene dominio, casos de uso (application) y puertos. NO debe depender de Android,
// Supabase, Ktor ni de ningún framework de UI (RNF16, RNF18). El compilador lo garantiza:
// este módulo no declara esas dependencias.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
