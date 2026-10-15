pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RobotCentinela"

// :core -> dominio, casos de uso y puertos (Kotlin/JVM puro, sin Android ni Supabase).
// :app  -> adaptadores (Supabase) y raíz de composición Android. Depende de :core, nunca al revés.
include(":core", ":app")
