package com.pedrobneto.easy.navigation.core.transition

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.core.adaptive.DefaultRegularSceneTransitions
import com.pedrobneto.easy.navigation.core.adaptive.PaneStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class RouteSceneTest {
    private val home = TestRoute("home")
    private val details = TestRoute("details")
    private val scope = SceneDecoratorStrategyScope<NavigationRoute>()

    @Test
    fun `custom scenes may reorder entries without changing the logical top`() {
        val entries = listOf(routeEntry(home), routeEntry(details))
        val original = TestScene("custom", entries.reversed())
        val strategies = routeSceneStrategies(listOf(SceneStrategy { original }), emptyList(), null)
        val captured = with(strategies.strategies.single()) { scope.calculateScene(entries) }
        val scene = assertIs<RouteScene>(captured)

        assertSame(details, scene.route)
        assertSame(home, scene.entries.last().transitionRoute)
        assertSame(original, scene.scene)
    }

    @Test
    fun `consumer decorators see original scene types and retain the snapshot`() {
        val entries = listOf(routeEntry(home))
        val original = TestScene("original", entries)
        val decorated = TestScene("decorated", entries)
        val strategies = routeSceneStrategies(
            listOf(SceneStrategy { original }),
            listOf(SceneDecoratorStrategy { scene ->
                assertSame(original, scene)
                decorated
            }),
            null,
        )
        val captured = assertIs<RouteScene>(with(strategies.strategies.single()) { scope.calculateScene(entries) })
        val final = assertIs<RouteScene>(with(strategies.decorators.single()) { scope.decorateScene(captured) })

        assertSame(decorated, final.scene)
        assertSame(home, final.route)
        assertEquals(captured.stack, final.stack)
    }

    @Test
    fun `different original scene classes remain distinct with equal keys`() {
        val first = snapshot(listOf(home), key = "same")
        val second = RouteScene(AlternateScene(first.scene), first.stack, null)
        assertNotEquals(first.key, second.key)
    }

    @Test
    fun `a rejected strategy retains navigation3 single pane fallback`() {
        val entries = listOf(routeEntry(home))
        val strategies = routeSceneStrategies(listOf(SceneStrategy { null }), emptyList(), null)
        val scene = assertIs<RouteScene>(with(strategies.strategies.single()) { scope.calculateScene(entries) })
        assertSame(home, scene.route)
        assertEquals(entries, scene.entries)
    }

    @Test
    fun `overlay scenes remain unwrapped`() {
        val entries = listOf(routeEntry(home), routeEntry(details))
        val overlay = TestOverlay(TestScene("modal", entries), entries.dropLast(1))
        val strategies = routeSceneStrategies(listOf(SceneStrategy { overlay }), emptyList(), null)
        val scene = with(strategies.strategies.single()) { scope.calculateScene(entries) }
        assertSame(overlay, scene)
        assertIs<OverlayScene<NavigationRoute>>(scene)
    }

    @Test
    fun `adaptive scene snapshots use the current entry rather than the last displayed entry`() {
        val entries = adaptiveEntries()
        val strategy = AdaptiveSceneStrategy(isUsingAdaptiveLayout = true)
        val strategies = routeSceneStrategies(listOf(strategy), emptyList(), null)
        val scene = assertIs<RouteScene>(with(strategies.strategies.single()) { scope.calculateScene(entries) })
        assertIs<AdaptiveSceneStrategy.DualPaneScene>(scene.scene)
        assertSame(details, scene.route)
        assertSame(home, scene.entries.last().transitionRoute)
    }

    @Test
    fun `public fallback helpers preserve adaptive transitions through scene wrappers`() {
        val entries = adaptiveEntries()
        val strategy = AdaptiveSceneStrategy(isUsingAdaptiveLayout = true)
        val plainScope = SceneStrategyScope<NavigationRoute>()
        val from = with(strategy) { plainScope.calculateScene(entries.take(1)) }
        val to = with(strategy) { plainScope.calculateScene(entries) }
        with(DefaultRegularSceneTransitions) {
            val regular = from transitionTo to
            val captured = RouteScene(from, listOf(home), null) transitionTo
                RouteScene(to, listOf(home, details), null)
            assertEquals(regular.targetContentEnter, captured.targetContentEnter)
            assertEquals(regular.initialContentExit, captured.initialContentExit)
        }
    }

    private fun adaptiveEntries(): List<NavEntry<NavigationRoute>> = listOf(
        routeEntry(home, metadata = mapOf(
            NavigationDirection.METADATA_ROUTE_KEY to home::class.qualifiedName.orEmpty(),
            NavigationDirection.METADATA_STRATEGY_KEY to PaneStrategy.Adaptive(),
        )),
        routeEntry(details, metadata = mapOf(
            NavigationDirection.METADATA_STRATEGY_KEY to PaneStrategy.Extra(
                PaneStrategy.Extra.PaneHost(TestRoute::class, .5f)
            ),
        )),
    )
}

private fun routeEntry(
    route: NavigationRoute,
    metadata: Map<String, Any> = emptyMap(),
): NavEntry<NavigationRoute> = NavEntry(
    route,
    metadata = metadata + (TRANSITION_ROUTE_METADATA_KEY to route),
) {}

private class AlternateScene(scene: Scene<NavigationRoute>) : Scene<NavigationRoute> by scene

private class TestOverlay(
    scene: Scene<NavigationRoute>,
    override val overlaidEntries: List<NavEntry<NavigationRoute>>,
) : OverlayScene<NavigationRoute>, Scene<NavigationRoute> by scene
