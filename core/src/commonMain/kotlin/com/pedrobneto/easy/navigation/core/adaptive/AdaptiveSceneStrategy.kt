package com.pedrobneto.easy.navigation.core.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

/**
 * A [SceneStrategy] that adapts the layout of scenes based on a [PaneStrategy].
 *
 * This strategy supports single-pane, dual-pane, and adaptive layouts. The decision on which
 * layout to use is determined by the [PaneStrategy] associated with the current and previous
 * navigation entries.
 *
 * @param orientation The [Orientation] to use for dual-pane layouts. Defaults to [Orientation.Horizontal].
 * @param divider A composable lambda to render a divider between two panes in a dual-pane layout.
 */
class AdaptiveSceneStrategy(
    private val isUsingAdaptiveLayout: Boolean,
    private val orientation: Orientation = Orientation.Horizontal,
    private val divider: @Composable (
        current: NavEntry<NavigationRoute>,
        previous: NavEntry<NavigationRoute>
    ) -> Unit = { _, _ -> },
) : SceneStrategy<NavigationRoute> {
    private val NavEntry<NavigationRoute>.paneRouteQualifiedName: String?
        get() = metadata[NavigationDirection.METADATA_ROUTE_KEY] as? String

    private val NavEntry<NavigationRoute>.paneStrategy: PaneStrategy?
        get() = metadata[NavigationDirection.METADATA_STRATEGY_KEY] as? PaneStrategy

    override fun SceneStrategyScope<NavigationRoute>.calculateScene(
        entries: List<NavEntry<NavigationRoute>>
    ): Scene<NavigationRoute> {
        val currentEntry = entries.last()
        val currentEntryPaneStrategy = currentEntry.paneStrategy

        val previousEntries = entries.dropLast(1)
        val previousEntry = previousEntries.lastOrNull()
        val previousEntryPaneStrategy = previousEntry?.paneStrategy

        val matchedHost = (currentEntryPaneStrategy as? PaneStrategy.Extra)?.hosts?.firstOrNull {
            it.route.qualifiedName == previousEntry?.paneRouteQualifiedName
        }?.takeIf { previousEntryPaneStrategy !is PaneStrategy.Single }

        return when (currentEntryPaneStrategy) {
            is PaneStrategy.Extra if isUsingAdaptiveLayout && previousEntry != null && matchedHost != null -> {
                DualPaneScene(
                    key = currentEntry.contentKey,
                    previousEntries = previousEntries,
                    currentEntry = currentEntry,
                    previousEntry = previousEntry,
                    entryRatio = matchedHost.ratio,
                    orientation = orientation,
                    divider = divider
                )
            }

            is PaneStrategy.Adaptive -> AdaptivePaneScene(
                key = currentEntry.contentKey,
                previousEntries = previousEntries,
                entry = currentEntry,
                strategy = currentEntryPaneStrategy,
                orientation = orientation,
            )

            else -> SinglePaneScene(
                key = currentEntry.contentKey,
                previousEntries = previousEntries,
                entry = currentEntry,
            )
        }
    }

    /**
     * Defines the orientation for dual-pane layouts.
     */
    enum class Orientation {
        /**
         * Panels are laid out horizontally, side by side.
         */
        Horizontal,

        /**
         * Panels are laid out vertically, one on top of the other.
         */
        Vertical
    }

    /**
     * A scene that displays a single panel.
     *
     * @property key The unique key for this scene.
     * @property previousEntries The list of entries before this scene.
     * @property entry The navigation entry to be displayed in this scene.
     */
    class SinglePaneScene internal constructor(
        override val key: Any,
        override val previousEntries: List<NavEntry<NavigationRoute>>,
        private val entry: NavEntry<NavigationRoute>,
    ) : Scene<NavigationRoute> {
        override val entries: List<NavEntry<NavigationRoute>> = listOf(entry)
        override val content: @Composable (() -> Unit) = { entry.Content() }
    }

    /**
     * A scene that displays a panel in an adaptive way, occupying a certain ratio of the screen.
     *
     * @property key The unique key for this scene.
     * @property previousEntries The list of entries before this scene.
     * @property entry The navigation entry to be displayed in this scene.
     * @property strategy The adaptive strategy that defines the ratio of the screen to occupy.
     */
    class AdaptivePaneScene internal constructor(
        override val key: Any,
        override val previousEntries: List<NavEntry<NavigationRoute>>,
        internal val entry: NavEntry<NavigationRoute>,
        private val strategy: PaneStrategy.Adaptive,
        private val orientation: Orientation,
    ) : Scene<NavigationRoute> {

        override val entries: List<NavEntry<NavigationRoute>> = listOf(entry)

        override val content: @Composable (() -> Unit) = {
            val spacerWeight by remember(key, entry, strategy) {
                derivedStateOf { (1f - strategy.ratio).takeIf { it > 0 } }
            }
            if (orientation == Orientation.Horizontal) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(strategy.ratio)) {
                        entry.Content()
                    }
                    spacerWeight?.let { Spacer(modifier = Modifier.weight(it)) }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(strategy.ratio)) {
                        entry.Content()
                    }
                    spacerWeight?.let { Spacer(modifier = Modifier.weight(it)) }
                }
            }
        }
    }

    /**
     * A scene that displays two panels side by side, either horizontally or vertically.
     *
     * @property key The unique key for this scene.
     * @property previousEntries The list of entries before this scene.
     * @property currentEntry The current navigation entry, displayed in the secondary panel.
     * @property entryRatio The extra panel ratio, determining its how much of the screen it occupies.
     * @property previousEntry The previous navigation entry, displayed in the primary panel.
     * @property orientation The layout orientation, either horizontal or vertical.
     * @param divider A composable lambda to render a divider between the two panels.
     */
    class DualPaneScene internal constructor(
        override val key: Any,
        override val previousEntries: List<NavEntry<NavigationRoute>>,
        internal val currentEntry: NavEntry<NavigationRoute>,
        internal val previousEntry: NavEntry<NavigationRoute>,
        private val entryRatio: Float,
        private val orientation: Orientation,
        private val divider: @Composable (
            current: NavEntry<NavigationRoute>,
            previous: NavEntry<NavigationRoute>
        ) -> Unit,
    ) : Scene<NavigationRoute> {

        fun isAfter(other: Scene<NavigationRoute>): Boolean =
            (other is DualPaneScene && other.currentEntry == previousEntry) ||
                    (other is AdaptivePaneScene && other.entry == previousEntry)

        override val entries: List<NavEntry<NavigationRoute>> =
            listOfNotNull(currentEntry, previousEntry)

        override val content: @Composable (() -> Unit) = {
            if (orientation == Orientation.Horizontal) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f - entryRatio)) {
                        previousEntry.Content()
                    }
                    divider.invoke(currentEntry, previousEntry)
                    Box(modifier = Modifier.weight(entryRatio)) {
                        currentEntry.Content()
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f - entryRatio)) {
                        previousEntry.Content()
                    }
                    divider.invoke(currentEntry, previousEntry)
                    Box(modifier = Modifier.weight(entryRatio)) {
                        currentEntry.Content()
                    }
                }
            }
        }
    }
}
