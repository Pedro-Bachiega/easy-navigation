package com.pedrobneto.easy.navigation.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File

internal class SourceWriter(private val output: File) {
    fun direction(destination: Destination) = with(destination) {
        val type = TypeSpec.objectBuilder(directionName)
            .addModifiers(KModifier.INTERNAL, KModifier.DATA)
            .superclass(ClassName("$CORE.model", "NavigationDirection"))
            .addSuperclassConstructorParameter("routeClass = %T::class", ClassName.bestGuess(route))
            .addSuperclassConstructorParameter("deeplinks = %L", list(deeplinks.map {
                CodeBlock.of("%T(%S)", ClassName("$CORE.model", "NavigationDeeplink"), it)
            }))
        if (global) type.addAnnotation(ClassName("$CORE.annotation", "GlobalScope"))
        parentRoute?.let { type.addSuperclassConstructorParameter("parentRouteClass = %T::class", ClassName.bestGuess(it)) }
        parentDeeplink?.let { type.addSuperclassConstructorParameter("parentDeeplink = %T(%S)", ClassName("$CORE.model", "NavigationDeeplink"), it) }
        if (modal) type.addSuperclassConstructorParameter("isModal = true")
        else type.addSuperclassConstructorParameter("paneStrategy = %L", paneCode(pane))
        type.addFunction(
            FunSpec.builder("register").addModifiers(KModifier.OVERRIDE)
                .addParameter("builder", ClassName("kotlinx.serialization.modules", "PolymorphicModuleBuilder")
                    .parameterizedByRoute())
                .addStatement("builder.subclass(%T::class, %T.serializer())", ClassName.bestGuess(route), ClassName.bestGuess(route))
                .build(),
        )
        val draw = FunSpec.builder("Draw").addModifiers(KModifier.OVERRIDE)
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("route", ClassName("$CORE.model", "NavigationRoute"))
        val functionName = MemberName(function.substringBeforeLast('.', ""), function.substringAfterLast('.'))
        if (parameter == null) draw.addStatement("%M()", functionName)
        else draw.addStatement("%M(%N = route as %T)", functionName, parameter, ClassName.bestGuess(route))
        type.addFunction(draw.build())
        FileSpec.builder(routePackage, directionName).addType(type.build()).build().writeTo(File(output, sourceSet))
    }

    fun registry(sourceSet: String, name: String, destinations: List<Destination>, scope: String?, kind: RegistryKind) {
        val type = TypeSpec.objectBuilder(name)
            .superclass(ClassName("$CORE.model", "DirectionRegistry"))
        if (scope != null) type.addAnnotation(AnnotationSpec.builder(ClassName("$CORE.annotation", "Scope")).addMember("%S", scope).build())
        if (kind == RegistryKind.EXPECT) type.addModifiers(KModifier.EXPECT)
        else {
            type.addModifiers(KModifier.DATA)
            if (kind == RegistryKind.ACTUAL) type.addModifiers(KModifier.ACTUAL)
            type.addSuperclassConstructorParameter("directions = %L", list(destinations.sortedBy { it.direction }.map {
                CodeBlock.of("%T", ClassName.bestGuess(it.direction))
            }))
        }
        FileSpec.builder(REGISTRY_PACKAGE, name).addType(type.build()).build().writeTo(File(output, sourceSet))
    }

    private fun paneCode(pane: Pane): CodeBlock {
        val type = ClassName("$CORE.adaptive", "PaneStrategy")
        return when (pane) {
            is Pane.Adaptive -> CodeBlock.of("%T.Adaptive(ratio = %Lf)", type, pane.ratio)
            Pane.Single -> CodeBlock.of("%T.Single", type)
            is Pane.Extra -> CodeBlock.of("%T.Extra(%L)", type, CodeBlock.builder().apply {
                pane.hosts.forEachIndexed { index, (host, ratio) ->
                    if (index > 0) add(", ")
                    add("%T.Extra.PaneHost(route = %T::class, ratio = %Lf)", type, ClassName.bestGuess(host), ratio)
                }
            }.build())
        }
    }

    private fun list(values: List<CodeBlock>): CodeBlock = if (values.isEmpty()) CodeBlock.of("emptyList()") else CodeBlock.builder().apply {
        add("listOf(\n").indent()
        values.forEach { add("%L,\n", it) }
        unindent().add(")")
    }.build()
}

internal enum class RegistryKind { EXPECT, ACTUAL, NORMAL }

private fun ClassName.parameterizedByRoute() = parameterizedBy(ClassName("$CORE.model", "NavigationRoute"))
