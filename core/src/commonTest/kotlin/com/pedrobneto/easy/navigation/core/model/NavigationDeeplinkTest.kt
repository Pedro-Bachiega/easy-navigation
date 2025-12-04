package com.pedrobneto.easy.navigation.core.model

import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NavigationDeeplinkTest {

    private val testDirections = listOf(
        object : NavigationDirection(
            deeplinks = listOf(NavigationDeeplink("/home")),
            routeClass = TestHomeRoute::class
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = listOf(NavigationDeeplink("/details/{id}")),
            routeClass = TestDetailsRoute::class,
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        },
        object : NavigationDirection(
            deeplinks = listOf(NavigationDeeplink("app://user/{userId}")),
            routeClass = TestUserRoute::class,
        ) {
            @Composable
            override fun Draw(route: NavigationRoute) {
            }
        }
    )

    private val json = Json { ignoreUnknownKeys = true }

    // region Property Tests
    @Test
    fun `parses deeplink with scheme host path and query params`() {
        val deeplink = NavigationDeeplink("nav://host/path/123?q1=v1&q2=v2")

        assertEquals("nav", deeplink.scheme)
        assertEquals("host", deeplink.host)
        assertEquals("/path/123", deeplink.path)
        assertEquals(
            mapOf("q1" to "v1", "q2" to "v2"),
            deeplink.queryParams
        )
        assertEquals("/host/path/123", deeplink.clean)
    }

    @Test
    fun `parses deeplink with just host`() {
        val deeplink = NavigationDeeplink("/home")

        assertNull(deeplink.scheme)
        assertEquals("home", deeplink.host)
        assertNull(deeplink.path)
        assertEquals(emptyMap(), deeplink.queryParams)
        assertEquals("/home", deeplink.clean)
    }

    @Test
    fun `parses deeplink with host and query`() {
        val deeplink = NavigationDeeplink("/home?a=b")

        assertNull(deeplink.scheme)
        assertEquals("home", deeplink.host)
        assertNull(deeplink.path)
        assertEquals(mapOf("a" to "b"), deeplink.queryParams)
        assertEquals("/home", deeplink.clean)
    }

    @Test
    fun `parses deeplink without path`() {
        val deeplink = NavigationDeeplink("app://host?q=p")

        assertEquals("app", deeplink.scheme)
        assertEquals("host", deeplink.host)
        assertNull(deeplink.path)
        assertEquals(mapOf("q" to "p"), deeplink.queryParams)
        assertEquals("/host", deeplink.clean)
    }

    @Test
    fun `throws exception for malformed deeplink`() {
        assertFailsWith<IllegalArgumentException> { NavigationDeeplink("host_only") }
        assertFailsWith<IllegalArgumentException> { NavigationDeeplink("://a") }
        assertFailsWith<IllegalArgumentException> { NavigationDeeplink("a:/") }
    }
    // endregion

    // region Resolve Tests
    @Test
    fun `resolve simple deeplink without args`() {
        val deeplink = NavigationDeeplink("/home")
        val route = deeplink.resolve(json, testDirections)
        assertEquals(TestHomeRoute, route)
    }

    @Test
    fun `resolve deeplink with path parameter`() {
        val deeplink = NavigationDeeplink("/details/99")
        val route = deeplink.resolve(json, testDirections) as TestDetailsRoute
        assertEquals(99L, route.id)
        assertEquals("default", route.source)
    }

    @Test
    fun `resolve deeplink with path and query parameters`() {
        val deeplink = NavigationDeeplink("/details/101?source=test")
        val route = deeplink.resolve(json, testDirections) as TestDetailsRoute
        assertEquals(101L, route.id)
        assertEquals("test", route.source)
    }

    @Test
    fun `resolve deeplink with scheme`() {
        val deeplink = NavigationDeeplink("app://user/pedro.bneto")
        val route = deeplink.resolve(json, testDirections) as TestUserRoute
        assertEquals("pedro.bneto", route.userId)
    }

    @Test
    fun `resolve throws for unknown deeplink`() {
        val deeplink = NavigationDeeplink("/unknown/path")
        val exception = assertFailsWith<IllegalArgumentException> {
            deeplink.resolve(json, testDirections)
        }
        assertEquals("No route found for deeplink '/unknown/path'", exception.message)
    }

    @Test
    fun `resolve throws for mismatched parameter type`() {
        val deeplink = NavigationDeeplink("/details/not-a-long")
        assertFailsWith<IllegalArgumentException> {
            deeplink.resolve(json, testDirections)
        }
    }
    // endregion

    @Serializable
    data object TestHomeRoute : NavigationRoute

    @Serializable
    data class TestDetailsRoute(val id: Long, val source: String = "default") : NavigationRoute

    @Serializable
    data class TestUserRoute(val userId: String) : NavigationRoute
}