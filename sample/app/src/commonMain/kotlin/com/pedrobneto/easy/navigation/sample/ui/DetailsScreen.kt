package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.sample.model.DetailsRoute
import com.pedrobneto.easy.navigation.sample.model.HomeRoute

@ExtraPane(host = HomeRoute::class, ratio = .7f)
@Deeplink("/details/{id}")
@Route(DetailsRoute::class)
@Composable
internal fun DetailsScreen(route: DetailsRoute) {
    val navigation = LocalNavigationController.current
    DetailsContent(
        route = route,
        onBackClick = navigation::safeNavigateUp,
        onExtraDetailsClick = { navigation.safeNavigateTo("/extra-details") }
    )
}

@Composable
private fun DetailsContent(
    route: DetailsRoute,
    onBackClick: () -> Unit = {},
    onExtraDetailsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
            Text("You are viewing details for item:", style = MaterialTheme.typography.bodySmall)
            Text(route.id.toString(), style = MaterialTheme.typography.bodyMedium)
            Text(
                "This is a sample detail screen. In a real app, you would display more information about the selected item here."
            )

            Spacer(Modifier.weight(1f))
            Button(onClick = onExtraDetailsClick) {
                Text("Show more")
            }
        }
    }
}

@Composable
@Preview
private fun DetailsScreenPreview() = MaterialTheme { DetailsContent(DetailsRoute(1)) }
