package com.pedrobneto.easy.navigation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.model.FirstScreenRoute
import com.pedrobneto.easy.navigation.model.Orientation
import com.pedrobneto.easy.navigation.model.WindowSize
import com.pedrobneto.easy.navigation.registry.SampleDirectionRegistry

@Composable
internal fun App() {
    MaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val screenInfo by getCurrentScreenInfo()
            if (!screenInfo.isValid) return@Scaffold

            val isUsingAdaptiveLayout by remember(screenInfo) {
                derivedStateOf { screenInfo.windowSize.isAtLeast(WindowSize.Medium) }
            }

            val orientation by remember(screenInfo) {
                derivedStateOf {
                    if (screenInfo.orientation == Orientation.Landscape) {
                        AdaptiveSceneStrategy.Orientation.Horizontal
                    } else {
                        AdaptiveSceneStrategy.Orientation.Vertical
                    }
                }
            }

            // Generated per scope using @Scope annotation
//            val (initialRoute, registries) = remember {
//                FirstScopedRoute to listOf(SampleScopeDirectionRegistry)
//            }

            // Generated per module using library gradle plugin
            val (initialRoute, registries) = remember {
                FirstScreenRoute to listOf(SampleDirectionRegistry)
            }

            // Generated aggregating all modules' registries using application gradle plugin
//            val (initialRoute, registries) = remember {
//                FirstScreenRoute to listOf(GlobalDirectionRegistry)
//            }

            Navigation(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                initialRoute = initialRoute,
                directionRegistries = registries,
                sceneStrategy = AdaptiveSceneStrategy(
                    isUsingAdaptiveLayout = isUsingAdaptiveLayout,
                    orientation = orientation
                )
            )
        }
    }
}
