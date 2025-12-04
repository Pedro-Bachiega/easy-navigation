package com.pedrobneto.easy.navigation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.model.FirstScopedRoute
import com.pedrobneto.easy.navigation.registry.SampleScopeDirectionRegistry

@Composable
internal fun App() {
    MaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Generated per scope using @Scope annotation
            val (initialRoute, registries) = remember {
                FirstScopedRoute to listOf(SampleScopeDirectionRegistry)
            }

            // Generated per module using library gradle plugin
//            val (initialRoute, registries) = remember {
//                FirstScreenRoute to listOf(SampleDirectionRegistry)
//            }

            // Generated aggregating all modules' registries using application gradle plugin
//            val (initialRoute, registries) = remember {
//                FirstScreenRoute to listOf(GlobalDirectionRegistry)
//            }

            Navigation(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                initialRoute = initialRoute,
                directionRegistries = registries
            )
        }
    }
}
