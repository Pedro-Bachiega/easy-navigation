package com.pedrobneto.easy.navigation.core.adaptive

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategyScope
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationDirection.Companion.METADATA_ROUTE_KEY
import com.pedrobneto.easy.navigation.core.model.NavigationDirection.Companion.METADATA_STRATEGY_KEY
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

private data object TestRoute : NavigationRoute
private data object HostRoute : NavigationRoute

private fun createTestNavEntry(
    key: String,
    strategy: PaneStrategy,
    route: NavigationRoute = TestRoute,
): NavEntry<NavigationRoute> = NavEntry(
    key = route,
    contentKey = key,
    metadata = mapOf(
        METADATA_ROUTE_KEY to route::class.qualifiedName.orEmpty(),
        METADATA_STRATEGY_KEY to strategy,
    )
) {}

class AdaptiveSceneStrategyTest {

    private val strategy = AdaptiveSceneStrategy(isUsingAdaptiveLayout = true)

    private val scope = SceneStrategyScope<NavigationRoute>()

    @Test
    fun `calculateScene with empty entries throws exception`() {
        val exception = try {
            strategy.run { scope.calculateScene(emptyList()) }
            null
        } catch (e: NoSuchElementException) {
            e
        }
        assertTrue(exception is NoSuchElementException)
    }

    @Test
    fun `given single entry with adaptive strategy then returns AdaptivePaneScene`() {
        val entry = createTestNavEntry("adaptive", PaneStrategy.Adaptive(1.0f))
        val scene = strategy.run { scope.calculateScene(listOf(entry)) }
        assertIs<AdaptiveSceneStrategy.AdaptivePaneScene>(scene)
    }

    @Test
    fun `given single entry with single strategy then returns SinglePaneScene`() {
        val entry = createTestNavEntry("single", PaneStrategy.Single)
        val scene = strategy.run { scope.calculateScene(listOf(entry)) }
        assertIs<AdaptiveSceneStrategy.SinglePaneScene>(scene)
    }

    @Test
    fun `given single entry with extra strategy then returns SinglePaneScene`() {
        val entry = createTestNavEntry(
            "extra",
            PaneStrategy.Extra(PaneStrategy.Extra.PaneHost(HostRoute::class, .5f))
        )
        val scene = strategy.run { scope.calculateScene(listOf(entry)) }
        assertIs<AdaptiveSceneStrategy.SinglePaneScene>(scene)
    }

    @Test
    fun `given two entries with current as extra and previous as adaptive and host matches then returns DualPaneScene`() {
        val previousEntry =
            createTestNavEntry("adaptive", PaneStrategy.Adaptive(1.0f), HostRoute)
        val currentEntry = createTestNavEntry(
            "extra",
            PaneStrategy.Extra(
                PaneStrategy.Extra.PaneHost(HostRoute::class, .5f)
            )
        )
        val scene = strategy.run { scope.calculateScene(listOf(previousEntry, currentEntry)) }
        assertIs<AdaptiveSceneStrategy.DualPaneScene>(scene)
    }

    @Test
    fun `given two entries with current as extra and previous as adaptive and host does not match then returns SinglePaneScene`() {
        val previousEntry =
            createTestNavEntry("adaptive", PaneStrategy.Adaptive(1.0f), TestRoute)
        val currentEntry = createTestNavEntry(
            "extra",
            PaneStrategy.Extra(PaneStrategy.Extra.PaneHost(HostRoute::class, .5f))
        )
        val scene = strategy.run { scope.calculateScene(listOf(previousEntry, currentEntry)) }
        assertIs<AdaptiveSceneStrategy.SinglePaneScene>(scene)
    }

    @Test
    fun `given two entries with current as extra and previous as single then returns SinglePaneScene`() {
        val previousEntry = createTestNavEntry("single", PaneStrategy.Single)
        val currentEntry = createTestNavEntry(
            "extra",
            PaneStrategy.Extra(PaneStrategy.Extra.PaneHost(HostRoute::class, .5f))
        )
        val scene = strategy.run { scope.calculateScene(listOf(previousEntry, currentEntry)) }
        assertIs<AdaptiveSceneStrategy.SinglePaneScene>(scene)
    }

    @Test
    fun `given two entries with current as adaptive then returns AdaptivePaneScene`() {
        val previousEntry = createTestNavEntry("previous", PaneStrategy.Adaptive(1.0f))
        val currentEntry = createTestNavEntry("current", PaneStrategy.Adaptive(.8f))
        val scene = strategy.run { scope.calculateScene(listOf(previousEntry, currentEntry)) }
        assertIs<AdaptiveSceneStrategy.AdaptivePaneScene>(scene)
    }

    @Test
    fun `given two entries with current as single then returns SinglePaneScene`() {
        val previousEntry = createTestNavEntry("previous", PaneStrategy.Adaptive(1.0f))
        val currentEntry = createTestNavEntry("current", PaneStrategy.Single)
        val scene = strategy.run { scope.calculateScene(listOf(previousEntry, currentEntry)) }
        assertIs<AdaptiveSceneStrategy.SinglePaneScene>(scene)
    }
}
