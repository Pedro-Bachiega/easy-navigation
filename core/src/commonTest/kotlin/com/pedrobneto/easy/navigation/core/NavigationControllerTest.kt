package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavBackStack
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationResult
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.modal.ModalScope
import com.pedrobneto.easy.navigation.core.transition.NavigationOperation
import com.pedrobneto.easy.navigation.core.transition.transitionRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
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
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object :
            NavigationDirection(
                deeplinks = emptyList(),
                routeClass = TestHomeRouteWithParentKClass::class,
                parentRouteClass = TestHomeRoute::class
            ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestHomeRouteWithParentDeeplink::class,
            parentDeeplink = NavigationDeeplink("/home")
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = listOf(NavigationDeeplink("/details/{id}")),
            routeClass = TestDetailsRoute::class,
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestSettingsRoute::class
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestModalRoute::class,
            isModal = true
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestExtraDetailsRoute::class
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestNestedRoute::class
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestHomeWithParameterizedParentClass::class,
            parentRouteClass = TestDetailsRoute::class
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = emptyList(),
            routeClass = TestHomeRouteWithUnresolvedParentDeeplink::class,
            parentDeeplink = NavigationDeeplink("/unresolved/deeplink")
        ) {
            override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) = Unit

            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
    )

    private val testRegistry = object : DirectionRegistry(testDirections) {}

    private lateinit var controller: NavigationController

    @BeforeTest
    fun setUp() {
        controller = NavigationController(
            backStack = NavBackStack(mutableStateListOf(TestHomeRoute)),
            directionRegistryList = listOf(testRegistry),
            json = Json { ignoreUnknownKeys = true }
        )
    }

    @Test
    fun `transition intent captures the whole NewStack mutation`() {
        controller.navigateTo(TestDetailsRoute(1))
        val before = controller.backStack.toList()
        controller.navigateTo(TestSettingsRoute, LaunchStrategy.NewStack)
        val change = assertNotNull(controller.transitionChange)
        assertEquals(before, change.before)
        assertEquals(listOf(TestSettingsRoute), change.after)
        assertEquals(NavigationOperation.NewStack, change.operation)
    }

    @Test
    fun `entry metadata retains the exact route instance for scene snapshots`() {
        val route = TestDetailsRoute(42)
        val entry = controller.directionProvider(route)
        assertTrue(entry.transitionRoute === route)
        assertEquals(TestDetailsRoute::class.qualifiedName, entry.metadata[NavigationDirection.METADATA_ROUTE_KEY])
    }

    @Test
    fun `transition intent captures multi pop before removing its source`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestSettingsRoute)
        val before = controller.backStack.toList()
        controller.popUpTo(TestHomeRoute)
        val change = assertNotNull(controller.transitionChange)
        assertEquals(before, change.before)
        assertEquals(listOf(TestHomeRoute), change.after)
        assertEquals(NavigationOperation.Pop, change.operation)
    }

    @Test
    fun `single top transition intent retains the old arguments`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestDetailsRoute(2), LaunchStrategy.SingleTop())
        val change = assertNotNull(controller.transitionChange)
        assertEquals(listOf(TestHomeRoute, TestDetailsRoute(1)), change.before)
        assertEquals(listOf(TestHomeRoute, TestDetailsRoute(2)), change.after)
        assertEquals(NavigationOperation.SingleTop, change.operation)
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
    fun `result launcher returns confirmed result and navigates up`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)

        assertTrue(launcher.navigateForResult(TestDetailsRoute(42)))
        assertFalse(launcher.navigateForResult(TestSettingsRoute))
        assertEquals(TestDetailsRoute(42), controller.currentRoute)

        val result = TestNavigationResult("saved")
        controller.navigateUpWithResult(result)

        assertEquals(TestHomeRoute, controller.currentRoute)
        assertEquals(listOf<NavigationResult<TestNavigationResult>>(NavigationResult.Confirmed(result)), results)
    }

    @Test
    fun `result launcher resolves deeplink payload and returns confirmed result`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)

        assertTrue(
            launcher.navigateForResult(
                deeplink = "/details/42?campaign=spring",
                payload = TestPayload(source = "launcher"),
            ),
        )
        assertEquals(
            TestDetailsRoute(id = 42, source = "launcher", campaign = "spring"),
            controller.currentRoute,
        )
        controller.navigateUpWithResult(TestNavigationResult("done"))

        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("done")),
            ),
            results,
        )
    }

    @Test
    fun `result launcher resolves a deeplink without payload`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)

        assertTrue(launcher.navigateForResult("/details/7"))
        assertEquals(TestDetailsRoute(7), controller.currentRoute)
        controller.navigateUpWithResult(TestNavigationResult("done"))

        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("done")),
            ),
            results,
        )
    }

    @Test
    fun `safe result launcher returns false for invalid deeplink`() {
        val launcherId = controller.allocateResultLauncherId()
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)

        assertFalse(launcher.safeNavigateForResult("/missing"))
        assertEquals(listOf(TestHomeRoute), controller.backStack.toList())
    }

    @Test
    fun `removing result destination delivers cancelled`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42))

        controller.navigateUp()

        assertEquals(TestHomeRoute, controller.currentRoute)
        assertEquals(listOf<NavigationResult<TestNavigationResult>>(NavigationResult.Cancelled), results)
    }

    @Test
    fun `popUpTo cancels a pending result destination`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42))

        controller.popUpTo(TestHomeRoute)

        assertEquals(TestHomeRoute, controller.currentRoute)
        assertEquals(listOf<NavigationResult<TestNavigationResult>>(NavigationResult.Cancelled), results)
    }

    @Test
    fun `NewStack cancels a pending result destination it removes`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42))

        controller.navigateTo(TestSettingsRoute, LaunchStrategy.NewStack)

        assertEquals(TestSettingsRoute, controller.currentRoute)
        assertEquals(listOf<NavigationResult<TestNavigationResult>>(NavigationResult.Cancelled), results)
    }

    @Test
    fun `chained launchers each receive only their own destination result`() {
        val firstLauncherId = controller.allocateResultLauncherId()
        val secondLauncherId = controller.allocateResultLauncherId()
        val firstResults = mutableListOf<NavigationResult<TestNavigationResult>>()
        val secondResults = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(firstLauncherId, TestNavigationResult.serializer()) {
            firstResults += it
        }
        controller.registerResultCallback(secondLauncherId, TestNavigationResult.serializer()) {
            secondResults += it
        }
        val firstLauncher = NavigationResultLauncher<TestNavigationResult>(controller, firstLauncherId)
        val secondLauncher = NavigationResultLauncher<TestNavigationResult>(controller, secondLauncherId)

        firstLauncher.navigateForResult(TestDetailsRoute(42))
        secondLauncher.navigateForResult(TestSettingsRoute)
        controller.navigateUpWithResult(TestNavigationResult("settings"))

        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("settings")),
            ),
            secondResults,
        )
        assertTrue(firstResults.isEmpty())
        assertEquals(TestDetailsRoute(42), controller.currentRoute)

        controller.navigateUpWithResult(TestNavigationResult("details"))

        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("details")),
            ),
            firstResults,
        )
    }

    @Test
    fun `safe navigate up with result confirms even when root cannot navigate up`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42), LaunchStrategy.NewStack)

        assertFalse(controller.safeNavigateUpWithResult(TestNavigationResult("still delivered")))

        assertEquals(TestDetailsRoute(42), controller.currentRoute)
        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("still delivered")),
            ),
            results,
        )
    }

    @Test
    fun `unsafe navigate up with result confirms before throwing at root`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42), LaunchStrategy.NewStack)

        assertFailsWith<IllegalStateException> {
            controller.navigateUpWithResult(TestNavigationResult("still delivered"))
        }

        assertEquals(TestDetailsRoute(42), controller.currentRoute)
        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("still delivered")),
            ),
            results,
        )
    }

    @Test
    fun `result state serializes pending request for process restoration`() {
        val launcherId = controller.allocateResultLauncherId()
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42))

        val stateJson = Json.encodeToString(NavigationResultState.serializer(), controller.navigationResultState.value)
        val restoredState = Json.decodeFromString<NavigationResultState>(stateJson)
        val restoredController = NavigationController(
            backStack = NavBackStack(mutableStateListOf(*controller.backStack.toTypedArray())),
            directionRegistryList = listOf(testRegistry),
            json = Json { ignoreUnknownKeys = true },
            navigationResultState = androidx.compose.runtime.mutableStateOf(restoredState),
        )
        val restoredResults = mutableListOf<NavigationResult<TestNavigationResult>>()
        restoredController.registerResultCallback(launcherId, TestNavigationResult.serializer()) {
            restoredResults += it
        }

        restoredController.navigateUpWithResult(TestNavigationResult("restored"))

        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("restored")),
            ),
            restoredResults,
        )
        assertEquals(TestHomeRoute, restoredController.currentRoute)
    }

    @Test
    fun `completed result is delivered after restored caller re-registers`() {
        val launcherId = controller.allocateResultLauncherId()
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestDetailsRoute(42))
        controller.navigateUpWithResult(TestNavigationResult("queued"))

        val stateJson = Json.encodeToString(NavigationResultState.serializer(), controller.navigationResultState.value)
        val restoredState = Json.decodeFromString<NavigationResultState>(stateJson)
        val restoredController = NavigationController(
            backStack = NavBackStack(mutableStateListOf(*controller.backStack.toTypedArray())),
            directionRegistryList = listOf(testRegistry),
            json = Json { ignoreUnknownKeys = true },
            navigationResultState = androidx.compose.runtime.mutableStateOf(restoredState),
        )
        val restoredResults = mutableListOf<NavigationResult<TestNavigationResult>>()

        restoredController.registerResultCallback(launcherId, TestNavigationResult.serializer()) {
            restoredResults += it
        }

        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("queued")),
            ),
            restoredResults,
        )
        assertFalse(restoredController.isResultLauncherPending(launcherId))
    }

    @Test
    fun `modal scope delegates result return`() {
        val launcherId = controller.allocateResultLauncherId()
        val results = mutableListOf<NavigationResult<TestNavigationResult>>()
        controller.registerResultCallback(launcherId, TestNavigationResult.serializer()) { results += it }
        val launcher = NavigationResultLauncher<TestNavigationResult>(controller, launcherId)
        launcher.navigateForResult(TestModalRoute)
        val modalScope = ModalScope(controller)

        modalScope.navigateUpWithResult(TestNavigationResult("modal"))

        assertEquals(TestHomeRoute, controller.currentRoute)
        assertEquals(
            listOf<NavigationResult<TestNavigationResult>>(
                NavigationResult.Confirmed(TestNavigationResult("modal")),
            ),
            results,
        )
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
    fun `modal scope delegates navigation operations to its controller`() {
        val modalScope = ModalScope(controller)

        modalScope.navigateTo(TestDetailsRoute(1))

        assertEquals(TestDetailsRoute(1), modalScope.currentRoute)
        assertEquals(controller.currentIndex, modalScope.currentIndex)
        assertTrue(modalScope.canNavigateUp)
        assertTrue(modalScope.safeNavigateUp())
        assertEquals(TestHomeRoute, controller.currentRoute)
    }

    @Test
    fun `modal scope delegates deeplink navigation operations`() {
        val modalScope = ModalScope(controller)

        modalScope.navigateTo("/details/2")

        assertEquals(TestDetailsRoute(2), controller.currentRoute)
        assertTrue(modalScope.safeNavigateTo("/details/3"))
        assertEquals(TestDetailsRoute(3), controller.currentRoute)
        assertFalse(modalScope.safeNavigateTo("/missing"))
        assertEquals(TestDetailsRoute(3), controller.currentRoute)
    }

    @Test
    fun `modal scope delegates pop up to operations`() {
        val modalScope = ModalScope(controller)
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestExtraDetailsRoute)

        modalScope.popUpTo(TestHomeRoute::class)

        assertEquals(listOf(TestHomeRoute), controller.backStack.toList())

        controller.navigateTo(TestDetailsRoute(2))
        assertTrue(modalScope.safePopUpTo(TestHomeRoute, inclusive = false))
        assertEquals(listOf(TestHomeRoute), controller.backStack.toList())
        assertFalse(modalScope.safePopUpTo(TestSettingsRoute))
    }

    @Test
    fun `modal scope delegates navigate up`() {
        val modalScope = ModalScope(controller)
        controller.navigateTo(TestDetailsRoute(1))

        modalScope.navigateUp()

        assertEquals(TestHomeRoute, controller.currentRoute)
    }

    @Test
    fun `navigateUp navigates to parent route and clears back stack when on empty back stack and parent route kclass was provided`() {
        controller.navigateTo(TestHomeRouteWithParentKClass, LaunchStrategy.NewStack)
        assertEquals(1, controller.backStack.size)
        controller.navigateUp()
        assertEquals(1, controller.backStack.size)
        assertEquals(TestHomeRoute, controller.backStack.last())
    }

    @Test
    fun `navigateUp navigates to parent route and clears back stack when on empty back stack and parent route deeplink was provided`() {
        controller.navigateTo(TestHomeRouteWithParentDeeplink, LaunchStrategy.NewStack)
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
    fun `navigateUp from nested root pops parent controller when parent can navigate up`() {
        val parentController = NavigationController(
            backStack = NavBackStack(mutableStateListOf(TestHomeRoute, TestSettingsRoute)),
            directionRegistryList = listOf(testRegistry),
            json = Json { ignoreUnknownKeys = true }
        )
        val childController = NavigationController(
            backStack = NavBackStack(mutableStateListOf(TestDetailsRoute(42))),
            directionRegistryList = listOf(testRegistry),
            parentController = parentController,
            json = Json { ignoreUnknownKeys = true }
        )

        childController.navigateUp()

        assertEquals(1, childController.backStack.size)
        assertEquals(TestDetailsRoute(42), childController.backStack.last())
        assertEquals(1, parentController.backStack.size)
        assertEquals(TestHomeRoute, parentController.backStack.last())
    }

    @Test
    fun `navigateUp from nested root restores parent route when parent moved past host destination`() {
        val parentController = NavigationController(
            backStack = NavBackStack(mutableStateListOf(TestHomeRoute, TestDetailsRoute(42))),
            directionRegistryList = listOf(testRegistry),
            json = Json { ignoreUnknownKeys = true }
        )
        val childController = NavigationController(
            backStack = NavBackStack(mutableStateListOf(TestNestedRoute(42))),
            directionRegistryList = listOf(testRegistry),
            parentController = parentController,
            json = Json { ignoreUnknownKeys = true }
        )

        parentController.navigateTo(TestExtraDetailsRoute)
        parentController.navigateTo(TestSettingsRoute)

        childController.navigateUp()

        assertEquals(1, childController.backStack.size)
        assertEquals(TestNestedRoute(42), childController.backStack.last())
        assertEquals(2, parentController.backStack.size)
        assertEquals(TestDetailsRoute(42), parentController.backStack.last())
    }

    @Test
    fun `navigateUp throws on empty back stack and parent route has parameters`() {
        controller.navigateTo(TestHomeWithParameterizedParentClass, LaunchStrategy.NewStack)
        assertEquals(1, controller.backStack.size)
        assertFailsWith<IllegalArgumentException> {
            controller.navigateUp()
        }
    }

    @Test
    fun `navigateUp throws on empty back stack and parent deeplinks has no matches`() {
        controller.navigateTo(TestHomeRouteWithUnresolvedParentDeeplink, LaunchStrategy.NewStack)
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
    fun `system back dismisses modal through the navigation controller`() {
        controller.navigateTo(TestModalRoute)

        controller.handleSystemBack()

        assertEquals(TestHomeRoute, controller.currentRoute)
    }

    @Test
    fun `system back delegates modal dismissal to the registered modal component`() {
        controller.navigateTo(TestModalRoute)
        val modalScope = ModalScope(controller)
        var backRequests = 0
        modalScope.setSystemBackRequestHandler { backRequests++ }
        controller.registerModalScope(modalScope)

        controller.handleSystemBack()

        assertEquals(1, backRequests)
        assertEquals(TestModalRoute, controller.currentRoute)
    }

    @Test
    fun `system back is delegated to the most recently registered scope for the current modal`() {
        controller.navigateTo(TestModalRoute)
        val firstScope = ModalScope(controller)
        val secondScope = ModalScope(controller)
        var firstRequests = 0
        var secondRequests = 0
        firstScope.setSystemBackRequestHandler { firstRequests++ }
        secondScope.setSystemBackRequestHandler { secondRequests++ }
        controller.registerModalScope(firstScope)
        controller.registerModalScope(secondScope)

        controller.handleSystemBack()

        assertEquals(0, firstRequests)
        assertEquals(1, secondRequests)
        assertEquals(TestModalRoute, controller.currentRoute)
    }

    @Test
    fun `unregistered modal scope no longer handles system back`() {
        controller.navigateTo(TestModalRoute)
        val modalScope = ModalScope(controller)
        var backRequests = 0
        modalScope.setSystemBackRequestHandler { backRequests++ }
        controller.registerModalScope(modalScope)
        controller.unregisterModalScope(modalScope)

        controller.handleSystemBack()

        assertEquals(0, backRequests)
        assertEquals(TestHomeRoute, controller.currentRoute)
    }

    @Test
    fun `clearing modal back handler restores controller fallback`() {
        controller.navigateTo(TestModalRoute)
        val modalScope = ModalScope(controller)
        var backRequests = 0
        modalScope.setSystemBackRequestHandler { backRequests++ }
        controller.registerModalScope(modalScope)
        modalScope.setSystemBackRequestHandler(null)

        controller.handleSystemBack()

        assertEquals(0, backRequests)
        assertEquals(TestHomeRoute, controller.currentRoute)
    }

    @Test
    fun `explicit navigate up dismisses a modal`() {
        controller.navigateTo(TestModalRoute)

        controller.navigateUp()

        assertEquals(TestHomeRoute, controller.currentRoute)
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
        assertEquals(3, controller.backStack.size)

        controller.navigateTo(TestDetailsRoute(2), LaunchStrategy.SingleTop(clearTop = true))
        assertEquals(2, controller.backStack.size)
        assertEquals(TestDetailsRoute(2), controller.backStack.last())
    }

    @Test
    fun `navigateTo with SingleTop clears every existing instance and pushes a new one to the top`() {
        controller.navigateTo(TestDetailsRoute(1))
        controller.navigateTo(TestDetailsRoute(2))
        controller.navigateTo(TestDetailsRoute(3))
        controller.navigateTo(TestSettingsRoute)
        assertEquals(5, controller.backStack.size)

        controller.navigateTo(TestDetailsRoute(4), LaunchStrategy.SingleTop())
        assertEquals(2, controller.backStack.size)
        assertEquals(TestDetailsRoute(4), controller.backStack.last())
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
    fun `navigateTo deeplink with payload merges path query and payload arguments`() {
        controller.navigateTo(
            deeplink = "/details/42?campaign=spring",
            payload = TestPayload(source = "push")
        )

        assertEquals(2, controller.backStack.size)
        assertEquals(
            TestDetailsRoute(id = 42, source = "push", campaign = "spring"),
            controller.backStack.last()
        )
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

    @Test
    fun `safeNavigateTo deeplink with payload returns false for non object payload`() {
        assertEquals(1, controller.backStack.size)
        assertFalse(controller.safeNavigateTo("/details/42", "not-an-object"))
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
        assertEquals(
            testDirections.first { it.routeClass == TestHomeRoute::class },
            controller.currentDirection
        )
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
    data object TestHomeRouteWithParentKClass : NavigationRoute

    @Serializable
    data object TestHomeRouteWithParentDeeplink : NavigationRoute

    @Serializable
    data object TestHomeWithParameterizedParentClass : NavigationRoute

    @Serializable
    data object TestHomeRouteWithUnresolvedParentDeeplink : NavigationRoute

    @Serializable
    data class TestDetailsRoute(
        val id: Long,
        val source: String = "default",
        val campaign: String = "none",
    ) : NavigationRoute

    @Serializable
    data class TestPayload(val source: String)

    @Serializable
    data class TestNavigationResult(val value: String)

    @Serializable
    data object TestSettingsRoute : NavigationRoute
    @Serializable
    data object TestModalRoute : NavigationRoute

    @Serializable
    data object TestExtraDetailsRoute : NavigationRoute

    @Serializable
    data class TestNestedRoute(val id: Long) : NavigationRoute

    @Serializable
    data object UnregisteredRoute : NavigationRoute
}
