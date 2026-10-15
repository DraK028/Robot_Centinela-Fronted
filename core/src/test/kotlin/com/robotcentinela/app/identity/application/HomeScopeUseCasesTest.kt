package com.robotcentinela.app.identity.application

import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.identity.domain.MembershipRole
import com.robotcentinela.app.identity.domain.MembershipStatus
import com.robotcentinela.app.identity.domain.Robot
import com.robotcentinela.app.identity.domain.SessionState
import com.robotcentinela.app.identity.ports.AuthPort
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.shared.NotInScopeException
import com.robotcentinela.app.testing.TestFrames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** HU01, RF01, RF02. */
class HomeScopeUseCasesTest {

    private val otherHome = TestFrames.HOME.copy(id = TestFrames.OTHER_HOME_ID, name = "Otro domicilio")

    private val port = object : HomeScopePort {
        override suspend fun memberships() = listOf(
            HomeMembership(TestFrames.HOME, MembershipRole("reader"), MembershipStatus.Active),
            HomeMembership(otherHome, MembershipRole("reader"), MembershipStatus.Inactive),
        )

        override suspend fun robotsOf(homeId: HomeId) = listOf(
            TestFrames.ROBOT,
            // Un robot de otro domicilio que el adaptador devolviera por error no entra al ámbito.
            Robot(TestFrames.OTHER_ROBOT_ID, TestFrames.OTHER_HOME_ID, "ajeno", "active"),
        )
    }

    private val currentScope = CurrentHomeScope()

    @Test
    fun `solo lista domicilios con membresia activa`() = runTest {
        val homes = ListAuthorizedHomes(port)()
        assertEquals(listOf(TestFrames.HOME_ID), homes.map { it.home.id })
    }

    @Test
    fun `seleccionar un domicilio activo fija el ambito con sus robots`() = runTest {
        val scope = SelectHomeScope(port, currentScope)(TestFrames.HOME_ID)

        assertEquals(listOf(TestFrames.ROBOT_ID), scope.robots.map { it.id })
        assertEquals(scope, currentScope.scope.value)
        assertEquals(scope, currentScope.requireRobot(TestFrames.ROBOT_ID))
    }

    @Test
    fun `no se puede seleccionar un domicilio con membresia inactiva`() = runTest {
        assertFailsWith<NotInScopeException> { SelectHomeScope(port, currentScope)(TestFrames.OTHER_HOME_ID) }
        assertNull(currentScope.scope.value)
    }

    @Test
    fun `un robot de otro domicilio queda fuera del ambito`() = runTest {
        SelectHomeScope(port, currentScope)(TestFrames.HOME_ID)
        assertFailsWith<NotInScopeException> { currentScope.requireRobot(TestFrames.OTHER_ROBOT_ID) }
    }

    @Test
    fun `cerrar sesion olvida el ambito seleccionado`() = runTest {
        val auth = object : AuthPort {
            override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Initializing)
            override suspend fun signInWithEmail(email: String, password: String) = Unit
            override suspend fun signOut() = Unit
            override suspend fun requestAccessRecovery(email: String) = Unit
        }
        SelectHomeScope(port, currentScope)(TestFrames.HOME_ID)

        SignOut(auth, currentScope)()

        assertNull(currentScope.scope.value)
    }
}
