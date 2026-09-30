package com.pedrobneto.easy.navigation.core.modal

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private data object BackgroundRoute : NavigationRoute
private data object ModalRoute : NavigationRoute

private fun entry(
    contentKey: String,
    route: NavigationRoute,
    isModal: Boolean = false,
): NavEntry<NavigationRoute> = NavEntry(
    key = route,
    contentKey = contentKey,
    metadata = buildMap {
        put(NavigationDirection.METADATA_ROUTE_KEY, route::class.qualifiedName.orEmpty())
        if (isModal) put(NavigationDirection.METADATA_MODAL_KEY, true)
    }
) {}

class ModalSceneStrategyTest {
    private val strategy = ModalSceneStrategy(SinglePaneSceneStrategy())
    private val scope = SceneStrategyScope<NavigationRoute>()

    @Test
    fun `non modal entry is not handled`() {
        val scene = strategy.run { scope.calculateScene(listOf(entry("background", BackgroundRoute))) }

        assertEquals(null, scene)
    }

    @Test
    fun `modal entry overlays the complete previous stack`() {
        val background = entry("background", BackgroundRoute)
        val modal = entry("modal", ModalRoute, isModal = true)

        val scene = strategy.run { scope.calculateScene(listOf(background, modal)) }

        assertIs<OverlayScene<NavigationRoute>>(scene)
        assertEquals(listOf(background, modal), scene.entries)
        assertEquals(listOf(background), scene.overlaidEntries)
    }

    @Test
    fun `modal can be stacked over another modal`() {
        val background = entry("background", BackgroundRoute)
        val firstModal = entry("first", ModalRoute, isModal = true)
        val secondModal = entry("second", ModalRoute, isModal = true)

        val scene = strategy.run {
            scope.calculateScene(listOf(background, firstModal, secondModal))
        }

        assertIs<OverlayScene<NavigationRoute>>(scene)
        assertEquals(listOf(background, firstModal, secondModal), scene.entries)
        assertEquals(listOf(background, firstModal), scene.overlaidEntries)
    }

    @Test
    fun `modal without a scene base fails clearly`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            strategy.run {
                scope.calculateScene(listOf(entry("modal", ModalRoute, isModal = true)))
            }
        }

        assertEquals(
            "Modal route cannot be displayed without a scene-base route. " +
                    "Navigate to it from another destination instead of using it as the root.",
            exception.message
        )
    }

    @Test
    fun `modal is not created when base scene strategy cannot handle previous entries`() {
        val strategyWithoutBaseScene = ModalSceneStrategy(
            SceneStrategy<NavigationRoute> { null }
        )

        val scene = strategyWithoutBaseScene.run {
            scope.calculateScene(
                listOf(
                    entry("background", BackgroundRoute),
                    entry("modal", ModalRoute, isModal = true),
                )
            )
        }

        assertEquals(null, scene)
    }
}
