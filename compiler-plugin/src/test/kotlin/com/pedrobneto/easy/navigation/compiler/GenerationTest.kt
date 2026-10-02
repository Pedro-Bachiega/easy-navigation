package com.pedrobneto.easy.navigation.compiler

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class GenerationTest {
    @Test
    fun `custom directories follow graph and preserve annotations route calls and serializers`() = fixture { root ->
        val common = source(root, "shared/Routes.kt", """
            package sample.routes
            class Home
            class Details
        """)
        val screens = source(root, "unusual/screens.kt", """
            package sample.screens
            import androidx.compose.runtime.Composable as UI
            import com.pedrobneto.easy.navigation.core.annotation.*
            import com.pedrobneto.easy.navigation.core.adaptive.*
            import sample.routes.Home
            @UI @Route(Home::class) @Deeplink("app://home") @AdaptivePane(ratio = .75f)
            fun HomeScreen() {}
        """)
        val android = source(root, "platform/screen.kt", """
            package sample.screens
            import androidx.compose.runtime.Composable
            import com.pedrobneto.easy.navigation.core.annotation.Route
            import com.pedrobneto.easy.navigation.core.annotation.Scope
            import com.pedrobneto.easy.navigation.core.modal.Modal
            import sample.routes.Details
            @Composable @Route(Details::class) @Scope("account") @Modal
            fun DetailsScreen(route: Details, label: String = "default") {}
        """)
        val graph = mapOf("shared" to emptyList(), "mobile" to listOf("shared"), "android" to listOf("mobile"), "ios" to listOf("mobile"))
        val output = File(root, "generated")
        generate(output, "app", graph, mapOf("shared" to listOf(common, screens), "android" to listOf(android)), listOf(GenerationTree("main", "shared", listOf("android", "ios"))))
        val home = File(output, "shared/sample/routes/HomeDirection.kt").readText()
        assertTrue("internal data object HomeDirection" in home)
        assertTrue("Home.serializer()" in home)
        assertTrue("HomeScreen()" in home)
        assertTrue("0.75f" in home)
        assertTrue("app://home" in home)
        val details = File(output, "android/sample/routes/DetailsDirection.kt").readText()
        assertTrue("isModal = true" in details)
        assertTrue("route = route as Details" in details)
        assertTrue("expect object AppDirectionRegistry" in File(output, "shared/com/pedrobneto/easy/navigation/registry/AppDirectionRegistry.kt").readText())
        assertTrue("actual data object AppDirectionRegistry" in File(output, "android/com/pedrobneto/easy/navigation/registry/AppDirectionRegistry.kt").readText())
        assertTrue("DetailsDirection" in File(output, "android/com/pedrobneto/easy/navigation/registry/AccountDirectionRegistry.kt").readText())
        assertFalse("DetailsDirection" in File(output, "ios/com/pedrobneto/easy/navigation/registry/AccountDirectionRegistry.kt").readText())
    }

    @Test
    fun `removing annotations removes obsolete directions and registries`() = fixture { root ->
        val file = source(root, "Screen.kt", """
            package sample
            import androidx.compose.runtime.Composable
            import com.pedrobneto.easy.navigation.core.annotation.Route
            class Home
            @Composable @Route(Home::class) fun HomeScreen() {}
        """)
        val output = File(root, "generated")
        val graph = mapOf("main" to emptyList<String>())
        val trees = listOf(GenerationTree("main", "main", listOf("main")))
        generate(output, "app", graph, mapOf("main" to listOf(file)), trees)
        assertTrue(output.walkTopDown().any { it.name == "HomeDirection.kt" })
        file.writeText("package sample\nclass Home")
        generate(output, "app", graph, mapOf("main" to listOf(file)), trees)
        assertFalse(output.walkTopDown().any { it.extension == "kt" })
    }

    @Test
    fun `required extra argument and collisions fail with source diagnostics`() = fixture { root ->
        val file = source(root, "Screen.kt", """
            package sample
            import androidx.compose.runtime.Composable
            import com.pedrobneto.easy.navigation.core.annotation.Route
            class Home
            @Composable @Route(Home::class) fun HomeScreen(title: String) {}
        """)
        val graph = mapOf("main" to emptyList<String>())
        val output = File(root, "generated")
        val trees = listOf(GenerationTree("main", "main", listOf("main")))
        val failure = assertFailsWith<IllegalArgumentException> { generate(output, "app", graph, mapOf("main" to listOf(file)), trees) }
        assertTrue("parameter 'title'" in failure.message.orEmpty())
        assertTrue("Screen.kt:" in failure.message.orEmpty())
        file.writeText(file.readText().replace("title: String", "title: String = \"default\"") + "\n@Composable @Route(Home::class) fun OtherScreen() {}")
        assertTrue("multiple destinations" in assertFailsWith<IllegalStateException> {
            generate(output, "app", graph, mapOf("main" to listOf(file)), trees)
        }.message.orEmpty())
    }

    @Test
    fun `aliases and source constants preserve route identity and annotation values`() = fixture { root ->
        val routes = source(root, "Routes.kt", """
            package sample
            class Home
            typealias Landing = Home
            const val SCHEME = "app"
            const val RATIO = 1f / 2f
        """)
        val screen = source(root, "Screen.kt", """
            package screen
            import androidx.compose.runtime.Composable
            import com.pedrobneto.easy.navigation.core.annotation.*
            import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
            import sample.Landing as Start
            import sample.SCHEME
            import sample.RATIO
            @Composable @Route(Start::class) @Deeplink(SCHEME + "://home") @AdaptivePane(RATIO)
            fun Screen(route: Start) {}
        """)
        val output = File(root, "generated")
        generate(output, "app", mapOf("main" to emptyList()), mapOf("main" to listOf(routes, screen)), listOf(GenerationTree("main", "main", listOf("main"))))
        val direction = File(output, "main/sample/HomeDirection.kt").readText()
        assertTrue("Home.serializer()" in direction)
        assertTrue("route = route as Home" in direction)
        assertTrue("app://home" in direction)
        assertTrue("0.5f" in direction)
    }

    private fun source(root: File, name: String, text: String): File = File(root, name).apply {
        parentFile.mkdirs()
        writeText(text.trimIndent())
    }

    private fun fixture(block: (File) -> Unit) {
        val root = createTempDirectory("navigation-generation").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
}
