package com.robotcentinela.app.identity.domain

import java.time.ZoneId

/**
 * Domicilio (`homes`, expediente §13.2).
 *
 * Por privacidad solo se guarda un nombre de trabajo, no la dirección postal (§15.1).
 *
 * @property timezone zona horaria del domicilio; se usa para convertir filtros locales a UTC (HU07).
 */
data class Home(val id: HomeId, val name: String, val timezone: ZoneId)

/**
 * Robot de un domicilio (`robots`, expediente §13.2).
 *
 * @property status estado de alta tal como lo reporta el servidor. El expediente no define su
 * catálogo, así que se conserva como texto.
 */
data class Robot(val id: RobotId, val homeId: HomeId, val publicCode: String, val status: String?)

/**
 * Rol de una membresía. El expediente (§13.2) menciona "rol reader o técnico" sin fijar los valores
 * exactos, así que se conserva el texto del servidor. La app NO autoriza nada por rol: los
 * permisos los aplica el servidor (RNF09, RB25).
 */
@JvmInline
value class MembershipRole(val raw: String)

/** Estado de una membresía (`memberships.status`: active o inactive, expediente §13.2). */
sealed interface MembershipStatus {
    val raw: String

    data object Active : MembershipStatus {
        override val raw = "active"
    }

    data object Inactive : MembershipStatus {
        override val raw = "inactive"
    }

    /** Valor no documentado; se conserva para no perder información. */
    data class Unknown(override val raw: String) : MembershipStatus

    companion object {
        fun fromRaw(raw: String): MembershipStatus = when (raw) {
            Active.raw -> Active
            Inactive.raw -> Inactive
            else -> Unknown(raw)
        }
    }
}

/** Vínculo entre la persona usuaria y un domicilio (`memberships`, expediente §13.2). */
data class HomeMembership(val home: Home, val role: MembershipRole, val status: MembershipStatus)

/**
 * Ámbito de consulta seleccionado explícitamente: un domicilio y sus robots (RF02, HU01).
 *
 * Toda consulta de telemetría, historial o alertas debe referirse a un robot de este ámbito.
 */
data class HomeScope(val home: Home, val robots: List<Robot>) {

    /** Indica si [robotId] pertenece al domicilio seleccionado. */
    fun contains(robotId: RobotId): Boolean = robots.any { it.id == robotId }
}
