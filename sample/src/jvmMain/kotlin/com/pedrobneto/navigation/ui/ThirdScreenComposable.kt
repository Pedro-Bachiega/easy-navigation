package com.pedrobneto.navigation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedrobneto.navigation.annotation.NavigationEntry
import com.pedrobneto.navigation.core.LocalNavigationController
import com.pedrobneto.navigation.core.launch.LaunchStrategy
import com.pedrobneto.navigation.model.SecondScreenRoute
import com.pedrobneto.navigation.model.ThirdScreenRoute

@Composable
@NavigationEntry(route = ThirdScreenRoute::class, deeplinks = ["/third"])
internal fun ThirdScreenComposable(
    modifier: Modifier = Modifier,
    thirdScreenRoute: ThirdScreenRoute
) = Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val navigation = LocalNavigationController.current

    Spacer(modifier = Modifier.weight(1f))
    Text(text = thirdScreenRoute.title)
    Text(text = thirdScreenRoute.description)
    Spacer(modifier = Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                navigation.popUpTo(
                    route = SecondScreenRoute(
                        title = "Second Screen",
                        description = "Backed using route"
                    )
                )
            },
            content = { Text("Back using route") }
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                navigation.navigateTo(
                    deeplink = "/first",
                    strategy = LaunchStrategy.SingleTop(true)
                )
            },
            content = { Text("Back using Deeplink") }
        )
    }
}
