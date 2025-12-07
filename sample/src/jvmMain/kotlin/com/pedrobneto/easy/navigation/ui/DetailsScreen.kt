package com.pedrobneto.easy.navigation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.model.DetailsRoute
import com.pedrobneto.easy.navigation.model.HomeRoute

@ExtraPane(host = HomeRoute::class, ratio = 0.5f)
@Deeplink("/details/{id}")
@Route(DetailsRoute::class)
@Composable
fun DetailsScreen(route: DetailsRoute) {
    val navigation = LocalNavigationController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = { navigation.safeNavigateUp() }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("You are viewing details for item:", style = MaterialTheme.typography.h6)
            Text(route.id.toString(), style = MaterialTheme.typography.h4)
            Text(
                "This is a sample detail screen. In a real app, you would display more information about the selected item here."
            )
        }
    }
}

@Composable
@Preview
private fun DetailsScreenPreview() = MaterialTheme { DetailsScreen(DetailsRoute(1)) }
