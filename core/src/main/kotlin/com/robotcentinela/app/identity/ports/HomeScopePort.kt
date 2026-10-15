package com.robotcentinela.app.identity.ports

import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.identity.domain.Robot

/**
 * Puerto de consulta de membresías y robots de la persona autenticada (RF02).
 *
 * El servidor filtra por RLS; el adaptador no debe aceptar un `user_id` arbitrario como
 * autorización (§13.5).
 */
interface HomeScopePort {

    /** Membresías de la persona autenticada. */
    suspend fun memberships(): List<HomeMembership>

    /** Robots visibles de un domicilio. */
    suspend fun robotsOf(homeId: HomeId): List<Robot>
}
