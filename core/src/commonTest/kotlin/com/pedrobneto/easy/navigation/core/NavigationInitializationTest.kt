package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.navigation3.runtime.NavBackStack
import com.pedrobneto.easy.navigation.core.extension.rememberNavBackStack
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class NavigationInitializationTest {
    private val registries = listOf(object : DirectionRegistry(listOf(DetailsDirection)) {})

    @Test
    fun initialDeeplinkResolvesLikeControllerNavigation() = withComposition { composition ->
        lateinit var controller: NavigationController
        composition.setContent {
            controller = rememberNavigationController("/details/12?source=query", registries)
        }
        val initial = controller.currentRoute
        controller.navigateTo("/details/12?source=query")
        assertEquals(Details(12, "query"), initial)
        assertEquals(initial, controller.currentRoute)
    }

    @Test
    fun initialPayloadUsesControllerArgumentPrecedence() = withComposition { composition ->
        lateinit var controller: NavigationController
        val payload = Payload(id = 42, source = "payload")
        composition.setContent {
            controller = rememberNavigationController(
                initialRoute = "/details/12?id=21&source=query",
                payload = payload,
                directionRegistries = registries,
            )
        }
        val initial = controller.currentRoute
        controller.navigateTo("/details/12?id=21&source=query", payload)
        assertEquals(Details(42, "payload"), initial)
        assertEquals(initial, controller.currentRoute)
    }

    @Test
    fun customJsonIsSharedWithSubsequentNavigation() = withComposition { composition ->
        lateinit var controller: NavigationController
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        composition.setContent {
            controller = rememberNavigationController(
                initialRoute = "/details/12?source=null",
                directionRegistries = registries,
                json = json,
            )
        }
        assertSame(json, controller.json)
        val initial = controller.currentRoute
        controller.navigateTo("/details/12?source=null")
        assertEquals(initial, controller.currentRoute)
    }

    @Test
    fun existingStackDoesNotRequireInitialRoute() = withComposition { composition ->
        val stack = NavBackStack<NavigationRoute>(Details(7))
        lateinit var controller: NavigationController
        composition.setContent {
            controller = rememberNavigationController(backStack = stack, directionRegistries = registries)
        }
        assertSame(stack, controller.backStack)
        assertEquals(Details(7), controller.currentRoute)
    }

    @Test
    fun recompositionIgnoresChangedInitialDeeplinkAndPayload() = withComposition { composition ->
        var deeplink = "/details/12"
        var payload = Payload(42, "first")
        lateinit var controller: NavigationController
        composition.setContent {
            controller = rememberNavigationController(deeplink, payload, registries)
        }
        val original = controller
        controller.navigateTo(Details(99))
        deeplink = "invalid"
        payload = Payload(100, "changed")
        composition.recompose()
        assertSame(original, controller)
        assertEquals(listOf(Details(42, "first"), Details(99)), controller.backStack.toList())
    }

    @Test
    fun restoredStackDoesNotResolveInvalidInitialDeeplink() {
        var registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        var deeplink = "/details/12"
        var payload = Payload(42, "saved")
        lateinit var stack: NavBackStack<NavigationRoute>
        lateinit var saved: Map<String, List<Any?>>
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                stack = rememberNavBackStack(deeplink, payload, registries)
            }
        }
        withComposition { composition ->
            composition.setContent(content)
            stack.add(Details(99))
            saved = registry.performSave()
        }
        registry = SaveableStateRegistry(saved, canBeSaved = { true })
        deeplink = "invalid"
        payload = Payload(100, "changed")
        withComposition { composition ->
            composition.setContent(content)
            assertEquals(listOf(Details(42, "saved"), Details(99)), stack.toList())
        }
    }

    @Test
    fun invalidInitialDeeplinksPropagateResolutionErrors() {
        for (deeplink in listOf("invalid", "/missing", "/details/not-a-number")) {
            assertFailsWith<IllegalArgumentException> {
                withComposition { composition ->
                    composition.setContent { rememberNavBackStack(deeplink, registries) }
                }
            }
        }
    }

    @Serializable
    private data class Details(val id: Int, val source: String = "default") : NavigationRoute

    @Serializable
    private data class Payload(val id: Int, val source: String)

    private object DetailsDirection : NavigationDirection(
        routeClass = Details::class,
        deeplinks = listOf(NavigationDeeplink("/details/{id}")),
    ) {
        override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) {
            builder.subclass(Details::class, Details.serializer())
        }

        @Composable
        override fun Draw(route: NavigationRoute) = Unit
    }

    private fun withComposition(block: (TestComposition) -> Unit) {
        val composition = TestComposition()
        try {
            block(composition)
        } finally {
            composition.dispose()
        }
    }

    private class TestComposition {
        private val recomposer = Recomposer(EmptyCoroutineContext)
        private val composition = ControlledComposition(UnitApplier(), recomposer)

        fun setContent(content: @Composable () -> Unit) {
            composition.composeContent(content)
            composition.applyChanges()
        }

        fun recompose() {
            composition.invalidateAll()
            composition.recompose()
            composition.applyChanges()
        }

        fun dispose() {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
