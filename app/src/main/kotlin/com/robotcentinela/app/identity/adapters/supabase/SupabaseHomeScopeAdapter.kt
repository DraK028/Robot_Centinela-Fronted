package com.robotcentinela.app.identity.adapters.supabase

import com.robotcentinela.app.identity.domain.Home
import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.identity.domain.MembershipRole
import com.robotcentinela.app.identity.domain.MembershipStatus
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.identity.domain.RobotId
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.shared.AccessDeniedException
import com.robotcentinela.app.shared.UtcInterval
import com.robotcentinela.app.shell.supabase.SupabaseResources
import com.robotcentinela.app.shell.supabase.SupabaseResources.Columns
import com.robotcentinela.app.shell.supabase.supabaseCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns as PostgrestColumns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneId
import java.util.UUID

/**
 * Adaptador de [HomeScopePort] sobre PostgREST (tablas `memberships`, `homes` y `robots`, §13.2).
 *
 * El filtro por `user_id` usa la sesión del SDK, no un valor elegido por el cliente. De todos modos
 * RLS es la protección real (§13.5).
 */
class SupabaseHomeScopeAdapter(private val supabase: SupabaseClient) : HomeScopePort {

    override suspend fun memberships(): List<HomeMembership> = supabaseCall("membresías") {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw AccessDeniedException("No hay una sesión válida")
        supabase.from(SupabaseResources.MEMBERSHIPS)
            .select(PostgrestColumns.raw("role, status, ${SupabaseResources.HOMES}(id, name, timezone)")) {
                filter { eq(Columns.USER_ID, userId) }
            }
            .decodeList<MembershipRowDto>()
            .map { it.toDomain() }
    }

    override suspend fun robotsOf(homeId: HomeId): List<Robot> = supabaseCall("robots del domicilio") {
        supabase.from(SupabaseResources.ROBOTS)
            .select(PostgrestColumns.raw("id, home_id, public_code, status")) {
                filter { eq(Columns.HOME_ID, homeId.value.toString()) }
            }
            .decodeList<RobotRowDto>()
            .map { it.toDomain() }
    }
}

/** Fila de `memberships` con su domicilio embebido. PROVISIONAL (§13.2). */
@Serializable
internal data class MembershipRowDto(
    val role: String,
    val status: String,
    val homes: HomeRowDto,
) {
    fun toDomain() = HomeMembership(homes.toDomain(), MembershipRole(role), MembershipStatus.fromRaw(status))
}

/** Fila de `homes`. PROVISIONAL (§13.2). */
@Serializable
internal data class HomeRowDto(
    val id: String,
    val name: String,
    val timezone: String? = null,
) {
    fun toDomain() = Home(HomeId(UUID.fromString(id)), name, zoneOrDefault(timezone))

    private fun zoneOrDefault(value: String?): ZoneId = try {
        value?.let(ZoneId::of) ?: UtcInterval.DEFAULT_ZONE
    } catch (e: DateTimeException) {
        // La base restringe la zona a valores válidos (§13.2). Si aun así llega una inválida, se usa
        // la zona por defecto de RNF13 en lugar de fallar la consulta completa.
        UtcInterval.DEFAULT_ZONE
    }
}

/** Fila de `robots`. PROVISIONAL (§13.2). */
@Serializable
internal data class RobotRowDto(
    val id: String,
    @SerialName("home_id") val homeId: String,
    @SerialName("public_code") val publicCode: String,
    val status: String? = null,
) {
    fun toDomain() = Robot(RobotId(UUID.fromString(id)), HomeId(UUID.fromString(homeId)), publicCode, status)
}
