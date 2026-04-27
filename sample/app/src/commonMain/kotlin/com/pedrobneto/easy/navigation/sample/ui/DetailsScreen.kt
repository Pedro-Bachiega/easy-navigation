package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.rememberNavigationController
import com.pedrobneto.easy.navigation.registry.AppDirectionRegistry
import com.pedrobneto.easy.navigation.sample.model.DetailsActivityRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsFaresRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsOverviewRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsRoute
import com.pedrobneto.easy.navigation.sample.model.HomeRoute

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun DetailsContent(
    route: DetailsRoute,
    onBackClick: () -> Unit = {},
    onExtraDetailsClick: () -> Unit = {}
) {
    val initialRoute = remember(route.id) { DetailsOverviewRoute(route.id) }
    val registries = remember { listOf(AppDirectionRegistry) }
    val subNavigationController = rememberNavigationController(
        initialRoute = initialRoute,
        directionRegistries = registries
    )
    val currentRoute = subNavigationController.currentRoute
    val tabs = remember(route.id) {
        listOf(
            DetailTab(
                title = "Overview",
                icon = Icons.Default.Info,
                route = DetailsOverviewRoute(route.id)
            ),
            DetailTab(
                title = "Activity",
                icon = Icons.AutoMirrored.Filled.EventNote,
                route = DetailsActivityRoute(route.id)
            ),
            DetailTab(
                title = "Fares",
                icon = Icons.Default.ConfirmationNumber,
                route = DetailsFaresRoute(route.id)
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Destination ${route.id}")
                        Text(
                            text = "Nested graph example",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onExtraDetailsClick) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Show more")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            subNavigationController.navigateTo(
                                route = tab.route,
                                strategy = LaunchStrategy.SingleTop(clearTop = true)
                            )
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Navigation(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            initialRoute = initialRoute,
            directionRegistries = registries,
            controller = subNavigationController
        )
    }
}

@Route(DetailsOverviewRoute::class)
@Deeplink("/details/{id}/overview")
@Composable
internal fun DetailsOverviewScreen(route: DetailsOverviewRoute) {
    DetailsPane(route.id) {
        HeroCard(
            title = "Harbor walk",
            subtitle = "A friendly first stop for route arguments, adaptive panes, and child navigation."
        )
        InfoGrid(
            items = listOf(
                "Distance" to "2.4 km",
                "Stops" to "6",
                "Mood" to "Calm",
                "Stack" to "Nested"
            )
        )
        Text(
            text = "This tab is the initial route of the detail sub-navigation graph.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Route(DetailsActivityRoute::class)
@Deeplink("/details/{id}/activity")
@Composable
internal fun DetailsActivityScreen(route: DetailsActivityRoute) {
    DetailsPane(route.id) {
        SectionTitle("Recent activity")
        ActivityRow("Opened from Home", "The parent graph pushed DetailsRoute(${route.id}).")
        ActivityRow("Selected Activity", "The child graph switched to DetailsActivityRoute.")
        ActivityRow("Try Back", "Back exits this tab before leaving the detail pane.")
    }
}

@Route(DetailsFaresRoute::class)
@Deeplink("/details/{id}/fares")
@Composable
internal fun DetailsFaresScreen(route: DetailsFaresRoute) {
    DetailsPane(route.id) {
        SectionTitle("Fare options")
        FareCard("Walk-up ticket", "$4", "Good for testing a plain child destination.")
        FareCard("Day pass", "$12", "Uses SingleTop when tapped repeatedly.")
        FareCard("Family bundle", "$18", "A little more real than Item 1, Item 2, Item 3.")
    }
}

@Composable
private fun DetailsPane(id: Long, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Route id: $id",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(value, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ActivityRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(12.dp)
                .fillMaxWidth(.04f)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FareCard(title: String, price: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(price, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private data class DetailTab(
    val title: String,
    val icon: ImageVector,
    val route: NavigationRoute
)

@Composable
@Preview
private fun DetailsScreenPreview() = MaterialTheme { DetailsContent(DetailsRoute(1)) }
