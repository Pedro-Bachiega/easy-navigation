package com.pedrobneto.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.pedrobneto.navigation.core.launch.LaunchStrategy
import com.pedrobneto.navigation.core.model.DirectionRegistry
import com.pedrobneto.navigation.core.model.NavigationDeeplink
import com.pedrobneto.navigation.core.model.NavigationDirection
import com.pedrobneto.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Serializable
data object TestHomeRoute : NavigationRoute

@Serializable
data class TestDetailsRoute(val id: Long) : NavigationRoute

@Serializable
data object TestSettingsRoute : NavigationRoute

class NavigationControllerTest {

    private val testDirections = listOf(
        object : NavigationDirection(TestHomeRoute::class, listOf(NavigationDeeplink("/home"))) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            TestDetailsRoute::class,
            listOf(NavigationDeeplink("/details/{id}"))
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(TestSettingsRoute::class, emptyList()) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        }
    )

    private val testRegistry = object : DirectionRegistry(testDirections) {}

    private lateinit var controller: NavigationController

    @BeforeTest
    fun setUp() {
        controller = NavigationController(
            backStack = mutableStateListOf(TestHomeRoute),
            directionRegistryList = listOf(testRegistry),
            json = Json { ignoreUnknownKeys = true }
        )
    }

    // region navigateUp / safeNavigateUp
    @Test
    fun `safeNavigateUp returns true and pops back stack when not on root`() {
        controller.navigateTo(TestDetailsRoute(1))

        assertEquals(2, controller.backStack.size)
        assertTrue(controller.safeNavigateUp())
        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `safeNavigateUp returns false and does not pop back stack when on root`() {
        assertEquals(1, controller.backStack.size)
        assertFalse(controller.safeNavigateUp())
        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `navigateUp pops the last entry from the back stack`() {
        controller.navigateTo(TestDetailsRoute(1))
        assertEquals(2, controller.backStack.size)
        controller.navigateUp()
        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `navigateUp throws on empty back stack`() {
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalStateException> {
            controller.navigateUp()
        }
    }
    // endregion

    // region navigateTo Route
    @Test
    fun `navigateTo with default strategy adds to back stack`() {
        controller.navigateTo(TestDetailsRoute(1))

        assertEquals(2, controller.backStack.size)
        assertEquals(TestDetailsRoute(1), controller.backStack.last())
    }

    @Test
    fun `navigateTo with NewTask(clearTask=true) clears back stack`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute, LaunchStrategy.NewTask(clearTask = true))

        assertEquals(1, controller.backStack.size)
        assertEquals(TestSettingsRoute, controller.backStack.last())
    }

    @Test
    fun `navigateTo with SingleTop does not duplicate top entry`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestDetailsRoute(1), LaunchStrategy.SingleTop())

        assertEquals(2, controller.backStack.size)
    }

    @Test
    fun `navigateTo with SingleTop(clearTop=true) brings existing entry to top`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)
        controller.navigateTo(TestDetailsRoute(2), LaunchStrategy.SingleTop(clearTop = true))

        assertEquals(2, controller.backStack.size)
        assertEquals(TestDetailsRoute(2), controller.backStack.last())
    }
    // endregion

    // region navigateTo/safeNavigateTo Deeplink
    @Test
    fun `navigateTo deeplink adds to back stack`() {
        controller.navigateTo("/details/42")

        assertEquals(2, controller.backStack.size)
        assertEquals(TestDetailsRoute(42), controller.backStack.last())
    }

    @Test
    fun `navigateTo deeplink throws for unknown deeplink`() {
        assertFailsWith<IllegalArgumentException> {
            controller.navigateTo("/unknown")
        }
    }

    @Test
    fun `safeNavigateTo deeplink returns true on success`() {
        assertEquals(1, controller.backStack.size)
        assertTrue(controller.safeNavigateTo("/details/42"))
        assertEquals(2, controller.backStack.size)
        assertEquals(TestDetailsRoute(42), controller.backStack.last())
    }

    @Test
    fun `safeNavigateTo deeplink returns false for unknown deeplink`() {
        assertEquals(1, controller.backStack.size)
        assertFalse(controller.safeNavigateTo("/unknown"))
        assertEquals(1, controller.backStack.size)
    }

    @Test
    fun `safeNavigateTo deeplink returns false for malformed deeplink`() {
        assertEquals(1, controller.backStack.size)
        assertFalse(controller.safeNavigateTo("details/not-a-link"))
        assertEquals(1, controller.backStack.size)
    }
    // endregion

    // region popUpTo
    @Test
    fun `popUpTo removes entries above target`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)

        assertEquals(3, controller.backStack.size)

        controller.popUpTo(TestHomeRoute)

        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `popUpTo with inclusive=true removes target as well`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)

        assertEquals(3, controller.backStack.size)

        controller.popUpTo(TestDetailsRoute(1), inclusive = true)

        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `popUpTo inclusive root throws exception`() {
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalStateException> {
            controller.popUpTo(TestHomeRoute, inclusive = true)
        }
    }
    // endregion
}