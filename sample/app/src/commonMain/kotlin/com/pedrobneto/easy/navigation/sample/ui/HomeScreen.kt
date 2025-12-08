package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsRoute
import com.pedrobneto.easy.navigation.sample.model.HomeRoute

@OptIn(ExperimentalMaterialApi::class)
@AdaptivePane(.3f)
@Deeplink("/home")
@Route(HomeRoute::class)
@Composable
internal fun HomeScreen() {
    val navigation = LocalNavigationController.current
    HomeContent(
        onNavigateToSettings = { navigation.navigateTo("/settings") },
        onNavigateToRoute = {
            navigation.navigateTo(route = it, strategy = LaunchStrategy.SingleTop())
        }
    )
}

@Composable
private fun HomeContent(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRoute: (NavigationRoute) -> Unit = {}
) {
    val items = remember { List(20) { "Item ${it + 1}" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Easy Navigation Sample") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(items, key = { it }) { item ->
                val itemId = item.substringAfter(" ").toLong()
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { onNavigateToRoute(DetailsRoute(id = itemId)) },
                    text = item
                )
            }
        }
    }
}

@Composable
@Preview
private fun HomeScreenPreview() = MaterialTheme { HomeContent() }
