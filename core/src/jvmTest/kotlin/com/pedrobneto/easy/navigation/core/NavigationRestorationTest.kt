package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.navigation3.runtime.NavBackStack
import com.pedrobneto.easy.navigation.core.NavigationInitializationTest.Details
import com.pedrobneto.easy.navigation.core.NavigationInitializationTest.DetailsDirection
import com.pedrobneto.easy.navigation.core.NavigationInitializationTest.Payload
import com.pedrobneto.easy.navigation.core.extension.rememberNavBackStack
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.test.Test
import kotlin.test.assertEquals

// Android host tests use stubbed Bundle methods. Verify serialized restoration on JVM,
// where SavedState has a working implementation; initial deeplink tests remain in commonTest.
class NavigationRestorationTest {
    private val registries = listOf(object : DirectionRegistry(listOf(DetailsDirection)) {})

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
}
