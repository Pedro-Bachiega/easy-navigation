package com.pedrobneto.easy.navigation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.model.DetailsRoute
import com.pedrobneto.easy.navigation.model.HomeRoute

@OptIn(ExperimentalMaterialApi::class)
@AdaptivePane(.5f)
@Deeplink("/home")
@Route(HomeRoute::class)
@Composable
fun HomeScreen() {
    val navigation = LocalNavigationController.current
    val items = remember { List(20) { "Item ${it + 1}" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Easy Navigation Sample") },
                actions = {
                    IconButton(onClick = { navigation.navigateTo("/settings") }) {
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
                ListItem(
                    modifier = Modifier.clickable {
                        navigation.navigateTo(
                            route = DetailsRoute(id = itemId),
                            strategy = LaunchStrategy.SingleTop()
                        )
                    }
                ) {
                    Text(text = item)
                }
            }
        }
    }
}

@Composable
@Preview
private fun HomeScreenPreview() = MaterialTheme { HomeScreen() }
