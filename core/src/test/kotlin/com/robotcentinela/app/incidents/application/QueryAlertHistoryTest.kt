package com.robotcentinela.app.incidents.application

import com.robotcentinela.app.identity.application.CurrentHomeScope
import com.robotcentinela.app.identity.application.SelectHomeScope
import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.identity.domain.MembershipRole
import com.robotcentinela.app.identity.domain.MembershipStatus
import com.robotcentinela.app.identity.ports.HomeScopePort
import com.robotcentinela.app.incidents.domain.Alert
import com.robotcentinela.app.incidents.domain.AlertState
import com.robotcentinela.app.incidents.ports.AlertFilter
import com.robotcentinela.app.incidents.ports.AlertQueryPort
import com.robotcentinela.app.shared.NotInScopeException
import com.robotcentinela.app.shared.PageRequest
import com.robotcentinela.app.testing.TestFrames
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** HU07, RF16. */
class QueryAlertHistoryTest {

    private val received = mutableListOf<AlertFilter>()
    private val alertPort = object : AlertQueryPort {
        override suspend fun alerts(filter: AlertFilter, page: PageRequest): List<Alert> {
            received += filter
            return emptyList()
        }
    }
    private val scopePort = object : HomeScopePort {
        override suspend fun memberships() =
            listOf(HomeMembership(TestFrames.HOME, MembershipRole("reader"), MembershipStatus.Active))

        override suspend fun robotsOf(homeId: HomeId) = listOf(TestFrames.ROBOT)
    }
    private val currentScope = CurrentHomeScope()
    private val useCase = QueryAlertHistory(alertPort, currentScope)

    @Test
    fun `convierte el filtro local a UTC con la zona del domicilio`() = runTest {
        SelectHomeScope(scopePort, currentScope)(TestFrames.HOME_ID)
        val day = LocalDate.of(2026, 10, 11)

        useCase(TestFrames.ROBOT_ID, PageRequest(50), day, day, states = setOf(AlertState.Open))

        val filter = received.single()
        assertEquals(Instant.parse("2026-10-11T06:00:00Z"), filter.interval?.start)
        assertEquals(Instant.parse("2026-10-12T06:00:00Z"), filter.interval?.endExclusive)
        assertEquals(setOf<AlertState>(AlertState.Open), filter.states)
    }

    @Test
    fun `sin fechas no limita el periodo`() = runTest {
        SelectHomeScope(scopePort, currentScope)(TestFrames.HOME_ID)
        useCase(TestFrames.ROBOT_ID, PageRequest(50))
        assertNull(received.single().interval)
    }

    @Test
    fun `no consulta robots de otro domicilio`() = runTest {
        SelectHomeScope(scopePort, currentScope)(TestFrames.HOME_ID)
        assertFailsWith<NotInScopeException> { useCase(TestFrames.OTHER_ROBOT_ID, PageRequest(50)) }
        assertEquals(0, received.size)
    }
}
