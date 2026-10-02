package fixture

import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data object MobileOnly : NavigationRoute

@Composable
@Route(MobileOnly::class)
fun MobileScreen() = Unit
