package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val items = remember {
        listOf(
            Destination(1, "Harbor walk", "A calm route for testing detail panes", "34 min", "Easy"),
            Destination(2, "Old town loop", "Deeplinks, back stack, and adaptive panes", "48 min", "Classic"),
            Destination(3, "Museum mile", "A denser item with friendly metadata", "26 min", "Indoor"),
            Destination(4, "Garden tram", "A compact trip for nested navigation", "18 min", "Scenic"),
            Destination(5, "Riverside market", "Great for checking list-to-detail flows", "41 min", "Popular"),
            Destination(6, "Sunset pier", "Shows how repeated selections use SingleTop", "22 min", "New"),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Easy Navigation")
                        Text(
                            text = "Adaptive routes and nested flows",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pick a destination",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "The list opens a detail pane. Inside it, the tabs are powered by a nested Navigation graph.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(items, key = { it }) { item ->
                DestinationRow(
                    destination = item,
                    onClick = { onNavigateToRoute(DetailsRoute(id = item.id)) }
                )
            }
        }
    }
}

@Composable
private fun DestinationRow(destination: Destination, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SampleColors.Mint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = destination.id.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(destination.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    destination.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${destination.duration} - ${destination.badge}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Destination(
    val id: Long,
    val title: String,
    val subtitle: String,
    val duration: String,
    val badge: String
)

@Composable
@Preview
private fun HomeScreenPreview() = MaterialTheme { HomeContent() }
