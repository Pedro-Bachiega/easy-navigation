package fixture

import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.annotation.*
import com.pedrobneto.easy.navigation.core.adaptive.*
import com.pedrobneto.easy.navigation.core.modal.Modal
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavigationRoute
@Serializable
data class Details(val id: Long) : NavigationRoute
@Serializable
data object Account : NavigationRoute
@Serializable
data object Dialog : NavigationRoute
@Serializable
data object Extra : NavigationRoute

var lastDrawnId: Long? = null

@Composable
@Route(Home::class)
@Deeplink("fixture://home")
@SinglePane
fun HomeScreen() = Unit

@Composable
@Route(Details::class)
@Deeplink("fixture://details/{id}")
@Deeplink("fixture://legacy/details/{id}")
@ParentRoute(Home::class)
@ParentDeeplink("fixture://home")
@AdaptivePane(ratio = .7f)
fun DetailsScreen(route: Details, label: String = "default") {
    check(label == "default")
    lastDrawnId = route.id
}

@Composable
@Route(Account::class)
@Scope("account")
fun AccountScreen() = Unit

@Composable
@Route(Dialog::class)
@Modal
fun DialogScreen() = Unit

@Composable
@Route(Extra::class)
@ExtraPane(host = Home::class, ratio = .4f)
fun ExtraScreen() = Unit

expect fun platformRouteName(): String
