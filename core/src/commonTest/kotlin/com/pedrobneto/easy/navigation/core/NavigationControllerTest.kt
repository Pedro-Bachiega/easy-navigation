package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NavigationControllerTest {

    private val testDirections = listOf(
        object : NavigationDirection(
            deeplinks = listOf(NavigationDeeplink("/home")),
            routeClass = TestHomeRoute::class
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object :
            NavigationDirection(
                deeplinks = emptyList(),
                routeClass = TestHomeRouteWithParent::class,
                parentRouteClass = TestHomeRoute::class
            ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = listOf(NavigationDeeplink("/details/{id}")),
            routeClass = TestDetailsRoute::class,
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestSettingsRoute::class
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestHomeWithParameterizedParent::class,
            parentRouteClass = TestDetailsRoute::class
        ) {
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
    fun `navigateUp navigates to parent route and clears back stack when on empty back stack and parent route was provided`() {
        controller.navigateTo(TestHomeRouteWithParent, LaunchStrategy.NewStack)
        assertEquals(1, controller.backStack.size)
        controller.navigateUp()
        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `navigateUp throws on empty back stack and no parent route provided`() {
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalStateException> {
            controller.navigateUp()
        }
    }

    @Test
    fun `navigateUp throws on empty back stack and parent route has parameters`() {
        controller.navigateTo(TestHomeWithParameterizedParent, LaunchStrategy.NewStack)
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalArgumentException> {
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
    fun `navigateTo with NewStack clears back stack`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute, LaunchStrategy.NewStack)

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
    fun `navigateTo with SingleTop with clearTop=true brings existing entry to top`() {
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

    @Test
    fun `popUpTo throws exception when route is not on the stack`() {
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalArgumentException> {
            controller.popUpTo(TestDetailsRoute(1))
        }
    }

    @Test
    fun `popUpTo with KClass removes entries above target`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)

        assertEquals(3, controller.backStack.size)

        controller.popUpTo(TestHomeRoute::class)

        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `popUpTo with KClass and inclusive=true removes target as well`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)

        assertEquals(3, controller.backStack.size)

        controller.popUpTo(TestDetailsRoute::class, inclusive = true)

        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `popUpTo with KClass inclusive root throws exception`() {
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalStateException> {
            controller.popUpTo(TestHomeRoute::class, inclusive = true)
        }
    }

    @Test
    fun `popUpTo with KClass throws exception when route is not on the stack`() {
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalArgumentException> {
            controller.popUpTo(TestDetailsRoute::class)
        }
    }

    @Test
    fun `safePopUpTo returns true on success`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)

        assertEquals(3, controller.backStack.size)

        assertTrue(controller.safePopUpTo(TestDetailsRoute(1)))

        assertEquals(2, controller.backStack.size)
    }

    @Test
    fun `safePopUpTo returns false on failure`() {
        assertEquals(1, controller.backStack.size)
        assertFalse(controller.safePopUpTo(TestHomeRoute, inclusive = true))
        assertEquals(1, controller.backStack.size)
    }
    // endregion

    @Test
    fun `directionProvider provides a NavEntry for a given NavigationRoute`() {
        val navEntry = controller.directionProvider(TestHomeRoute)
        assertNotNull(navEntry)
    }

    @Test
    fun `currentDirection returns the direction for the current route`() {
        assertEquals(testDirections.first { it.routeClass == TestHomeRoute::class }, controller.currentDirection)
    }

    @Test
    fun `currentDirection throws error if no direction is found`() {
        controller.navigateTo(UnregisteredRoute, LaunchStrategy.NewStack)
        assertFailsWith<IllegalStateException> {
            controller.currentDirection
        }
    }

    @Serializable
    data object TestHomeRoute : NavigationRoute

    @Serializable
    data object TestHomeRouteWithParent : NavigationRoute

    @Serializable
    data object TestHomeWithParameterizedParent : NavigationRoute

    @Serializable
    data class TestDetailsRoute(val id: Long) : NavigationRoute

    @Serializable
    data object TestSettingsRoute : NavigationRoute

    @Serializable
    data object UnregisteredRoute : NavigationRoute
}
