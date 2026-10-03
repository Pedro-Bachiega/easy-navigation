package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.pedrobneto.easy.navigation.sample.model.ExtraDetailsRoute

@ExtraPane(host = DetailsRoute::class, ratio = .3f)
@Deeplink("/extra-details")
@Route(ExtraDetailsRoute::class)
@Composable
internal fun ExtraDetailsScreen() {
    val navigation = LocalNavigationController.current
    ExtraDetailsContent(onBackClick = navigation::safeNavigateUp)
}

@Composable
private fun ExtraDetailsContent(onBackClick: () -> Unit = {}) = Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
        TopAppBar(
            title = { Text("More details") },
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
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Text("Extra pane", style = MaterialTheme.typography.titleLarge)
            Text(
                "This destination is hosted alongside details on larger windows, then behaves like a normal screen on compact layouts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Use it for related content, supporting actions, or a preview that should stay connected to the selected route.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
@Preview
private fun DetailsScreenPreview() = MaterialTheme { ExtraDetailsContent() }
