package com.robotcentinela.app.shell.supabase

import com.robotcentinela.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Configuración de conexión a Supabase.
 *
 * Se lee de `local.properties`, que no se versiona, a través de `BuildConfig`:
 * - `supabase.url`: URL HTTPS del proyecto.
 * - `supabase.publishableKey`: clave **pública** del cliente (anon o publishable).
 *
 * Nunca se acepta una clave privilegiada: `service_role` o una clave secreta `sb_secret_...`. Esas
 * claves no deben llegar al teléfono (§7.3: "Nunca enviar service_role, una clave secreta de Supabase
 * o credenciales de PostgreSQL al ESP32, web o móvil"; RNF08).
 */
class SupabaseConfig private constructor(val url: String, val publishableKey: String) {

    /** No incluye la clave, para que no termine en logs (RNF08). */
    override fun toString(): String = "SupabaseConfig(url=$url, publishableKey=***)"

    companion object {

        /** Construye la configuración desde `BuildConfig` (valores de `local.properties`). */
        fun fromBuildConfig(): SupabaseConfig =
            create(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)

        /**
         * Valida y construye la configuración.
         *
         * @throws IllegalStateException si falta un valor, la URL no usa HTTPS (RNF08: TLS en
         * transporte cloud) o la clave es privilegiada.
         */
        fun create(url: String, publishableKey: String): SupabaseConfig {
            val cleanUrl = url.trim()
            val cleanKey = publishableKey.trim()
            check(cleanUrl.isNotEmpty()) { "Falta supabase.url en local.properties" }
            check(cleanKey.isNotEmpty()) { "Falta supabase.publishableKey en local.properties" }
            check(cleanUrl.startsWith("https://")) { "supabase.url debe usar HTTPS" }
            check(!isPrivilegedKey(cleanKey)) {
                "supabase.publishableKey es una clave privilegiada; la app solo admite la clave pública"
            }
            return SupabaseConfig(cleanUrl, cleanKey)
        }

        /** Detecta claves secretas nuevas (`sb_secret_`) y JWT legacy con rol `service_role`. */
        internal fun isPrivilegedKey(key: String): Boolean {
            if (key.startsWith("sb_secret_")) return true
            val parts = key.split('.')
            if (parts.size != 3) return false
            val role = runCatching {
                val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
                Json.parseToJsonElement(payload).jsonObject["role"]?.jsonPrimitive?.content
            }.getOrNull()
            return role == "service_role"
        }
    }
}
