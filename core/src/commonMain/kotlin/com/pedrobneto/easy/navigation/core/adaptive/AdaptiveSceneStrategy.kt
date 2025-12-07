package com.pedrobneto.easy.navigation.core.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
    private val orientation: Orientation = Orientation.Horizontal,
    private val divider: @Composable (
        current: NavEntry<NavigationRoute>,
        previous: NavEntry<NavigationRoute>
    ) -> Unit = { _, _ -> },
) : SceneStrategy<NavigationRoute> {
    private val NavEntry<NavigationRoute>.paneStrategy
        get() = runCatching {
            metadata[NavigationDirection.METADATA_STRATEGY_KEY] as PaneStrategy
        }.getOrDefault(PaneStrategy.Adaptive)

    override fun SceneStrategyScope<NavigationRoute>.calculateScene(
        entries: List<NavEntry<NavigationRoute>>
    ): Scene<NavigationRoute> {
        val currentEntry = entries.last()
        val currentEntryPaneStrategy = currentEntry.paneStrategy

        val previousEntries = entries.dropLast(1)
        val previousEntry = previousEntries.lastOrNull()
        val previousEntryPaneStrategy = previousEntry?.paneStrategy

        return when (currentEntryPaneStrategy) {
            is PaneStrategy.Extra if previousEntry != null && previousEntryPaneStrategy !is PaneStrategy.Single -> {
                DualPaneScene(
                    key = currentEntry.contentKey,
                    previousEntries = previousEntries,
                    currentEntry = currentEntry,
                    currentEntryStrategy = currentEntryPaneStrategy,
                    previousEntry = previousEntry,
                    orientation = orientation,
                    divider = divider
                )
            }

            is PaneStrategy.Adaptive -> AdaptivePaneScene(
                key = currentEntry.contentKey,
                previousEntries = previousEntries,
                entry = currentEntry,
                strategy = currentEntryPaneStrategy,
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
    internal data class SinglePaneScene(
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
    internal data class AdaptivePaneScene(
        override val key: Any,
        override val previousEntries: List<NavEntry<NavigationRoute>>,
        private val entry: NavEntry<NavigationRoute>,
        private val strategy: PaneStrategy.Adaptive,
    ) : Scene<NavigationRoute> {
        override val entries: List<NavEntry<NavigationRoute>> = listOf(entry)
        override val content: @Composable (() -> Unit) = {
            Box(modifier = Modifier.fillMaxWidth(strategy.ratio)) {
                entry.Content()
            }
        }
    }

    /**
     * A scene that displays two panels side by side, either horizontally or vertically.
     *
     * @property key The unique key for this scene.
     * @property previousEntries The list of entries before this scene.
     * @property currentEntry The current navigation entry, displayed in the secondary panel.
     * @property currentEntryStrategy The extra panel strategy for the current entry, defining the size ratio.
     * @property previousEntry The previous navigation entry, displayed in the primary panel.
     * @property orientation The layout orientation, either horizontal or vertical.
     * @param divider A composable lambda to render a divider between the two panels.
     */
    internal data class DualPaneScene(
        override val key: Any,
        override val previousEntries: List<NavEntry<NavigationRoute>>,
        private val currentEntry: NavEntry<NavigationRoute>,
        private val currentEntryStrategy: PaneStrategy.Extra,
        private val previousEntry: NavEntry<NavigationRoute>,
        private val orientation: Orientation,
        private val divider: @Composable (
            current: NavEntry<NavigationRoute>,
            previous: NavEntry<NavigationRoute>
        ) -> Unit,
    ) : Scene<NavigationRoute> {

        override val entries: List<NavEntry<NavigationRoute>> =
            listOfNotNull(currentEntry, previousEntry)
        override val content: @Composable (() -> Unit) = {
            if (orientation == Orientation.Horizontal) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f - currentEntryStrategy.ratio)) {
                        previousEntry.Content()
                    }
                    divider(currentEntry, previousEntry)
                    Box(modifier = Modifier.weight(currentEntryStrategy.ratio)) {
                        currentEntry.Content()
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f - currentEntryStrategy.ratio)) {
                        previousEntry.Content()
                    }
                    divider(currentEntry, previousEntry)
                    Box(modifier = Modifier.weight(currentEntryStrategy.ratio)) {
                        currentEntry.Content()
                    }
                }
            }
        }
    }
}
