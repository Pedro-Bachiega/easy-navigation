package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
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
import java.io.ByteArrayOutputStream
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(KspExperimental::class)
class DirectionProcessTest {

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val logger = mockk<KSPLogger>(relaxed = true)
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
        val result = codeGenerator.createDirection(logger, function, "testModule", true)

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
        val outputStream = ByteArrayOutputStream()
        every {
            codeGenerator.createNewFile(
                any(),
                any(),
                capture(fileNameSlot),
                any()
            )
        } returns outputStream

        // WHEN
        val result = codeGenerator.createDirection(logger, function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals("SampleRouteDirection", fileNameSlot.captured)

        val content = outputStream.toString().normalizeLineEndings()
        assertContains(content, "internal data object SampleRouteDirection : NavigationDirection(")
        assertContains(content, "routeClass = DirectionProcessTest.SampleRoute::class")
        assertContains(content, "deeplinks = emptyList()")
        assertContains(content, "paneStrategy = PaneStrategy.Adaptive(ratio = 1.0f)")
        assertContains(
            content,
            "builder.subclass(DirectionProcessTest.SampleRoute::class, DirectionProcessTest.SampleRoute.serializer())"
        )
        assertContains(content, "SampleScreen()")
    }

    @Test
    fun `GIVEN a function with ParentRoute WHEN processing THEN Direction contains parent route`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            routeAnnotations = listOf(Route(SampleRoute::class)),
            parentRouteAnnotations = listOf(ParentRoute(SampleParentRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(logger, function, "testModule", true)

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
        val result = codeGenerator.createDirection(logger, function, "testModule", true)

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
        val result = codeGenerator.createDirection(logger, function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals(scopes, result.scopes)
    }

    @Test
    fun `GIVEN Route annotation WHEN processing THEN it should read route from KSP type argument`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            routeAnnotations = listOf(Route(SampleRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(logger, function, "testModule", true)

        // THEN
        assertNotNull(result)
        assertEquals(
            "com.pedrobneto.easy.navigation.processor.library.DirectionProcessTest",
            result.routePackageName
        )
        assertEquals("SampleRoute", result.routeClassName)
    }

    @Test
    fun `GIVEN not multiplatform and file in commonMain WHEN processing THEN it should return null`() {
        // GIVEN
        val function = mockFunctionDeclaration(
            filePath = "/some/path/commonMain/MyFile.kt",
            routeAnnotations = listOf(Route(SampleRoute::class))
        )

        // WHEN
        val result = codeGenerator.createDirection(logger, function, "jvm", isMultiplatformWithSingleTarget = false)

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
        val result = codeGenerator.createDirection(logger, function, "jvm", isMultiplatformWithSingleTarget = false)

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
        val result = codeGenerator.createDirection(logger, function, "jvm", isMultiplatformWithSingleTarget = true)

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

        every { function.annotations } returns buildList {
            add(mockAnnotationRaw("androidx.compose.runtime.Composable"))
            addAll(routeAnnotations.map { it.toKsAnnotation(Route::class, "value") })
            addAll(parentRouteAnnotations.map { it.toKsAnnotation(ParentRoute::class, "value") })
            addAll(deeplinkAnnotations.map { it.toKsAnnotation(Deeplink::class, "value") })
            addAll(scopeAnnotations.map { it.toKsAnnotation(Scope::class, "value") })
        }.asSequence()

        return function
    }

    private fun Route.toKsAnnotation(annotationClass: KClass<out Annotation>, argumentName: String) =
        mockAnnotation(
            annotationClass = annotationClass,
            argumentName = argumentName,
                argumentValue = value.qualifiedName.orEmpty().let(::mockType)
        )

    private fun ParentRoute.toKsAnnotation(annotationClass: KClass<out Annotation>, argumentName: String) =
        mockAnnotation(
            annotationClass = annotationClass,
            argumentName = argumentName,
            argumentValue = value.qualifiedName.orEmpty().let(::mockType)
        )

    private fun Deeplink.toKsAnnotation(annotationClass: KClass<out Annotation>, argumentName: String) =
        mockAnnotation(annotationClass, argumentName, value)

    private fun Scope.toKsAnnotation(annotationClass: KClass<out Annotation>, argumentName: String) =
        mockAnnotation(annotationClass, argumentName, value)

    private fun mockAnnotation(
        annotationClass: KClass<out Annotation>,
        argumentName: String,
        argumentValue: Any?
    ): KSAnnotation {
        val annotation = mockk<KSAnnotation>()
        val shortName = mockName(annotationClass.simpleName.orEmpty())
        val qualifiedName = mockName(annotationClass.qualifiedName.orEmpty())
        val declaration = mockk<KSDeclaration>()
        val type = mockk<KSType>()
        val typeReference = mockk<KSTypeReference>()
        val argument = mockk<KSValueArgument>()

        every { annotation.shortName } returns shortName
        every { annotation.annotationType } returns typeReference
        every { typeReference.resolve() } returns type
        every { type.declaration } returns declaration
        every { declaration.qualifiedName } returns qualifiedName
        every { annotation.arguments } returns listOf(argument)
        every { argument.name } returns mockName(argumentName)
        every { argument.value } returns argumentValue

        return annotation
    }

    private fun mockAnnotationRaw(qualifiedClassName: String): KSAnnotation {
        val annotation = mockk<KSAnnotation>()
        val typeReference = mockk<KSTypeReference>()
        val type = mockk<KSType>()
        val declaration = mockk<KSDeclaration>()

        every { annotation.shortName } returns mockName(qualifiedClassName.substringAfterLast('.'))
        every { annotation.annotationType } returns typeReference
        every { typeReference.resolve() } returns type
        every { type.declaration } returns declaration
        every { declaration.qualifiedName } returns mockName(qualifiedClassName)
        every { annotation.arguments } returns emptyList()

        return annotation
    }

    private fun mockType(qualifiedClassName: String): KSType {
        val type = mockk<KSType>()
        val declaration = mockk<KSClassDeclaration>()
        val packageName = qualifiedClassName.substringBeforeLast('.')
        val navigationRouteReference = mockk<KSTypeReference>()
        val navigationRouteType = mockk<KSType>()
        val navigationRouteDeclaration = mockk<KSClassDeclaration>()

        every { type.declaration } returns declaration
        every { declaration.packageName } returns mockName(packageName)
        every { declaration.simpleName } returns mockName(qualifiedClassName.substringAfterLast('.'))
        every { declaration.qualifiedName } returns mockName(qualifiedClassName)
        every { declaration.annotations } returns sequenceOf(mockAnnotationRaw("kotlinx.serialization.Serializable"))
        every { declaration.superTypes } returns if (qualifiedClassName == NavigationRoute::class.qualifiedName) {
            emptySequence()
        } else {
            sequenceOf(navigationRouteReference)
        }
        every { navigationRouteReference.resolve() } returns navigationRouteType
        every { navigationRouteType.declaration } returns navigationRouteDeclaration
        every { navigationRouteDeclaration.qualifiedName } returns mockName(NavigationRoute::class.qualifiedName.orEmpty())
        every { navigationRouteDeclaration.superTypes } returns emptySequence()

        return type
    }

    private fun mockName(value: String): KSName {
        val name = mockk<KSName>()
        every { name.asString() } returns value
        every { name.getShortName() } returns value.substringAfterLast('.')
        return name
    }

    private fun String.normalizeLineEndings(): String =
        this.replace("\r\n", "\n").replace("\t", "    ")

    private fun assertContains(actual: String, expected: String) {
        kotlin.test.assertTrue(
            actual.contains(expected),
            "Expected generated content to contain:\n$expected\n\nActual:\n$actual"
        )
    }

    data object SampleRoute : NavigationRoute
    data object SampleParentRoute : NavigationRoute
}
