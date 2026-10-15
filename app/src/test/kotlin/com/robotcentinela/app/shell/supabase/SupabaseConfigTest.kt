package com.robotcentinela.app.shell.supabase

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/** §7.3 y RNF08: solo HTTPS y solo la clave pública del cliente. */
class SupabaseConfigTest {

    private fun jwtWithRole(role: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = encoder.encodeToString("""{"iss":"supabase","role":"$role"}""".toByteArray())
        return "$header.$payload.firma-de-prueba"
    }

    @Test
    fun `acepta una URL HTTPS con clave publica`() {
        val config = SupabaseConfig.create("https://ejemplo.supabase.co", "sb_publishable_prueba")
        assertEquals("https://ejemplo.supabase.co", config.url)
    }

    @Test
    fun `acepta un JWT legacy con rol anon`() {
        SupabaseConfig.create("https://ejemplo.supabase.co", jwtWithRole("anon"))
    }

    @Test
    fun `rechaza valores vacios`() {
        assertFailsWith<IllegalStateException> { SupabaseConfig.create("", "sb_publishable_prueba") }
        assertFailsWith<IllegalStateException> { SupabaseConfig.create("https://ejemplo.supabase.co", " ") }
    }

    @Test
    fun `rechaza transporte sin TLS`() {
        assertFailsWith<IllegalStateException> {
            SupabaseConfig.create("http://ejemplo.supabase.co", "sb_publishable_prueba")
        }
    }

    @Test
    fun `rechaza claves privilegiadas`() {
        assertFailsWith<IllegalStateException> {
            SupabaseConfig.create("https://ejemplo.supabase.co", "sb_secret_prueba")
        }
        assertFailsWith<IllegalStateException> {
            SupabaseConfig.create("https://ejemplo.supabase.co", jwtWithRole("service_role"))
        }
    }

    @Test
    fun `toString no expone la clave`() {
        val config = SupabaseConfig.create("https://ejemplo.supabase.co", "sb_publishable_prueba")
        assertFalse(config.toString().contains("sb_publishable_prueba"))
    }
}
