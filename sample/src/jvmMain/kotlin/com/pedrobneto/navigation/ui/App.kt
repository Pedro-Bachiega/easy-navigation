package com.pedrobneto.navigation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pedrobneto.navigation.core.Navigation
import com.pedrobneto.navigation.model.FirstScreenRoute
import com.pedrobneto.navigation.registry.GlobalDirectionRegistry
import com.pedrobneto.navigation.registry.SampleDirectionRegistry

@Composable
internal fun App() {
    MaterialTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Generated per module using module processor plugin
            val contextualRegistries = remember {
                listOf(SampleDirectionRegistry)
            }

            // Generated aggregating all modules' registries using application processor plugin
            val globalRegistry = remember {
                listOf(GlobalDirectionRegistry)
            }

            Navigation(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                initialRoute = FirstScreenRoute,
                directionRegistries = globalRegistry
            )
        }
    }
}
