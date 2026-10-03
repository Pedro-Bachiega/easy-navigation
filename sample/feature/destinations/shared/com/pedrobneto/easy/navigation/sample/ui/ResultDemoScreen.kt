package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.SinglePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.sample.model.ResultDemoRoute

@SinglePane
@Deeplink("/result-demo/{query}")
@Route(ResultDemoRoute::class)
@Composable
internal fun ResultDemoScreen(route: ResultDemoRoute) {
    val navigation = LocalNavigationController.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Navigation result") },
                navigationIcon = {
                    IconButton(onClick = navigation::safeNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Argument received: ${route.query}", style = MaterialTheme.typography.titleMedium)
            Text(
                "This screen can confirm a value or return Cancelled by navigating back normally.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { navigation.navigateUpWithResult("Selected: ${route.query}") }) {
                Text("Confirm result")
            }
            Button(onClick = { navigation.safeNavigateUp() }) {
                Text("Cancel")
            }
        }
    }
}
