package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSValueParameter
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.annotation.Scope
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Before
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(KspExperimental::class)
class DirectionProcessTest {

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val outputStream = mockk<OutputStream>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic("com.google.devtools.ksp.UtilsKt")
    }

    @Test
    fun `GIVEN a function without Route annotation WHEN processing THEN it should return null`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            routeAnnotations = emptyList()
        )
        every { function.getAnnotationsByType(Route::class) } returns emptySequence()

        // WHEN
        val result = codeGenerator.createDirection(function, "testModule", true)

        // THEN
        assertNull(result)
    }

    @Test
    fun `GIVEN a function with Route annotation WHEN processing THEN it should generate a Direction file`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            functionName = "SampleScreen",
            packageName = "com.sample.ui",
            routeAnnotations = listOf(Route(SampleRoute::class))
        )
        val fileNameSlot = slot<String>()
        val fileContentSlot = slot<ByteArray>()
        every {
            codeGenerator.createNewFile(
                any(),
                any(),
                capture(fileNameSlot),
                any()
            )
        } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

        // WHEN
        val result = codeGenerator.createDirection(function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals("SampleRouteDirection", fileNameSlot.captured)

        val expectedContent = """
            package com.pedrobneto.easy.navigation.processor.library.DirectionProcessTest

            import androidx.compose.runtime.Composable
            import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
            import com.pedrobneto.easy.navigation.core.model.NavigationDirection
            import com.pedrobneto.easy.navigation.core.model.NavigationRoute
            import com.sample.ui.SampleScreen

            internal data object SampleRouteDirection : NavigationDirection(
                routeClass = SampleRoute::class,
                deeplinks = emptyList()
            ) {
                @Composable
                override fun Draw(route: NavigationRoute) {
                    SampleScreen()
                }
            }
        """.trimIndent()
        assertEquals(
            expectedContent.normalizeLineEndings(),
            String(fileContentSlot.captured).normalizeLineEndings()
        )
    }

    @Test
    fun `GIVEN a function with ParentRoute WHEN processing THEN Direction contains parent route`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            routeAnnotations = listOf(Route(SampleRoute::class)),
            parentRouteAnnotations = listOf(ParentRoute(SampleParentRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals(
            "com.pedrobneto.easy.navigation.processor.library.DirectionProcessTest",
            result.parentRoutePackageName
        )
        assertEquals("SampleParentRoute", result.parentRouteClassName)
    }

    @Test
    fun `GIVEN a function with Deeplink annotations WHEN processing THEN Direction contains deeplinks`() {
        // GIVEN
        val deeplinks = listOf("app://deeplink1", "app://deeplink2")
        val function = mockFunctionDeclaration(
            routeAnnotations = listOf(Route(SampleRoute::class)),
            deeplinkAnnotations = deeplinks.map { Deeplink(it) }
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals(deeplinks, result.deeplinks)
    }

    @Test
    fun `GIVEN a function with Scope annotations WHEN processing THEN Direction contains scopes`() {
        // GIVEN
        val scopes = listOf("scope1", "scope2")
        val function = mockFunctionDeclaration(
            routeAnnotations = listOf(Route(SampleRoute::class)),
            scopeAnnotations = scopes.map { Scope(it) }
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals(scopes, result.scopes)
    }

    @Test
    fun `GIVEN Route KClass access throws ClassNotFoundException WHEN processing THEN it should parse class name from exception`() {
        // GIVEN
        val exception =
            ClassNotFoundException("Some message with ClassNotFoundException: com.sample.navigation.FromExceptionRoute")
        val routeAnnotation = mockk<Route>()
        every { routeAnnotation.value } throws exception

        val function = mockFunctionDeclaration(
            routeAnnotations = listOf(routeAnnotation)
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals("com.sample.navigation", result.routePackageName)
        assertEquals("FromExceptionRoute", result.routeClassName)
    }

    @Test
    fun `GIVEN not multiplatform and file in commonMain WHEN processing THEN it should return null`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            filePath = "/some/path/commonMain/MyFile.kt",
            routeAnnotations = listOf(Route(SampleRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "jvm", isMultiplatformWithSingleTarget = false)

        // THEN
        assertNull(result)
    }

    @Test
    fun `GIVEN not multiplatform and file not in commonMain WHEN processing THEN it should process`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            filePath = "/some/path/jvmMain/MyFile.kt",
            routeAnnotations = listOf(Route(SampleRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "jvm", isMultiplatformWithSingleTarget = false)

        // THEN
        assertNotNull(result)
    }

    @Test
    fun `GIVEN is multiplatform with single target WHEN processing THEN it should process regardless of path`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            filePath = "/some/path/commonMain/MyFile.kt",
            routeAnnotations = listOf(Route(SampleRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(function, "jvm", isMultiplatformWithSingleTarget = true)

        // THEN
        assertNotNull(result)
    }

    private fun mockFunctionDeclaration(
        functionName: String = "TestFunction",
        packageName: String = "com.test",
        filePath: String = "commonMain/com/test/Test.kt",
        parameters: List<KSValueParameter> = emptyList(),
        routeAnnotations: List<Route> = emptyList(),
        parentRouteAnnotations: List<ParentRoute> = emptyList(),
        deeplinkAnnotations: List<Deeplink> = emptyList(),
        scopeAnnotations: List<Scope> = emptyList()
    ): KSFunctionDeclaration {
        val function = mockk<KSFunctionDeclaration>()
        val file = mockk<KSFile>()
        val funcName = mockk<KSName>()
        val packName = mockk<KSName>()

        every { function.simpleName } returns funcName
        every { funcName.asString() } returns functionName
        every { function.packageName } returns packName
        every { packName.asString() } returns packageName
        every { function.containingFile } returns file
        every { file.filePath } returns filePath
        every { function.parameters } returns parameters

        every { function.getAnnotationsByType(Route::class) } returns routeAnnotations.asSequence()
        every { function.getAnnotationsByType(ParentRoute::class) } returns parentRouteAnnotations.asSequence()
        every { function.getAnnotationsByType(Deeplink::class) } returns deeplinkAnnotations.asSequence()
        every { function.getAnnotationsByType(Scope::class) } returns scopeAnnotations.asSequence()

        return function
    }

    private fun String.normalizeLineEndings(): String =
        this.replace("\r\n", "\n").replace("\t", "    ")

    data object SampleRoute : NavigationRoute
    data object SampleParentRoute : NavigationRoute
}
