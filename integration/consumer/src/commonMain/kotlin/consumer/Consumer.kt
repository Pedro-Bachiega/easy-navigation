package consumer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
import com.pedrobneto.easy.navigation.registry.AccountDirectionRegistry
import com.pedrobneto.easy.navigation.registry.FeatureDirectionRegistry
import fixture.Home

// This module does not apply the compiler plugin: published metadata must expose both registries.
val registries: List<DirectionRegistry> = listOf(FeatureDirectionRegistry, AccountDirectionRegistry)

@Composable
fun ConsumerNavigation() {
    Navigation(modifier = Modifier, initialRoute = Home, directionRegistries = registries)
}
