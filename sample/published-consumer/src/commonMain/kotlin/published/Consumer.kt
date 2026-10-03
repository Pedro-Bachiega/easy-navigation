package published

import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.registry.PublishedConsumerDirectionRegistry
import kotlinx.serialization.Serializable

@Serializable
data object PublishedRoute : NavigationRoute
@Composable
@Route(PublishedRoute::class)
fun PublishedScreen() = Unit

val registries = listOf(PublishedConsumerDirectionRegistry)
