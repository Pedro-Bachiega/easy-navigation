package fixture

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import com.pedrobneto.easy.navigation.core.adaptive.PaneStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.registry.AccountDirectionRegistry
import com.pedrobneto.easy.navigation.registry.FeatureDirectionRegistry
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

class RegistryBehaviorTest {
    @Test
    fun registryIncludesSharedAndOnlyTheCurrentPlatform() {
        val names = FeatureDirectionRegistry.directions.map { it.routeClass.simpleName }.toSet()
        val sampleRoutes = setOf("HomeRoute", "DetailsRoute", "DetailsOverviewRoute", "DetailsActivityRoute",
            "DetailsFaresRoute", "ExtraDetailsRoute", "SettingsRoute", "ModalDemoRoute", "SpringModalDemoRoute", "ResultDemoRoute")
        val expected = sampleRoutes + setOf("Home", "Details", "Dialog", "Extra", platformRouteName()) +
            if (platformRouteName() == "DesktopOnly") emptySet() else setOf("MobileOnly")
        assertEquals(expected, names)
        assertEquals(listOf(Account::class), AccountDirectionRegistry.directions.map { it.routeClass })
    }

    @Test
    fun metadataAndSerializationMatchTheAnnotationContract() {
        val details = FeatureDirectionRegistry.directions.single { it.routeClass == Details::class }
        assertEquals(Home::class, details.parentRouteClass)
        assertEquals("fixture://home", details.parentDeeplink?.raw)
        assertEquals(listOf("fixture://details/{id}", "fixture://legacy/details/{id}"), details.deeplinks.map { it.raw })
        assertEquals(PaneStrategy.Adaptive(.7f), details.paneStrategy)
        assertTrue(FeatureDirectionRegistry.directions.single { it.routeClass == Dialog::class }.isModal)
        assertEquals(PaneStrategy.Single, FeatureDirectionRegistry.directions.single { it.routeClass == Home::class }.paneStrategy)
        assertEquals(PaneStrategy.Extra(PaneStrategy.Extra.PaneHost(Home::class, .4f)), FeatureDirectionRegistry.directions.single { it.routeClass == Extra::class }.paneStrategy)
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(NavigationRoute::class) {
                    FeatureDirectionRegistry.registerAll(this)
                    AccountDirectionRegistry.registerAll(this)
                }
            }
        }
        val serializer = PolymorphicSerializer(NavigationRoute::class)
        val route: NavigationRoute = Details(42)
        assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
    }

    @Test
    fun generatedDrawUsesComposeTransformationAndPassesTheRoute() {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = ControlledComposition(UnitApplier(), recomposer)
        try {
            lastDrawnId = null
            composition.composeContent { DetailsDirection.Draw(Details(73)) }
            composition.applyChanges()
            assertEquals(73L, lastDrawnId)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }
}

private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
}
