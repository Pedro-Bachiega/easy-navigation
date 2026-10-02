package com.pedrobneto.easy.navigation.compiler

import java.io.File
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNamedFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.getKClassArgument
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.fullyExpandedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class NavigationCompilerPlugin : CompilerPluginRegistrar() {
    override val pluginId: String = PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messages = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        FirExtensionRegistrarAdapter.registerExtension(NavigationFirRegistrar(messages))
    }
}

private class NavigationFirRegistrar(private val messages: MessageCollector) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> NavigationCheckers(session, messages) }
    }
}

private class NavigationCheckers(session: FirSession, messages: MessageCollector) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val namedFunctionCheckers = setOf(DestinationChecker(messages))
    }
}

private class DestinationChecker(private val messages: MessageCollector) : FirNamedFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val session = context.session
        val annotations = declaration.annotations.associateBy { it.toAnnotationClassId(session)?.asSingleFqName()?.asString() }
        if (annotations.keys.none { it in DESTINATION_ANNOTATIONS }) return
        val routeAnnotation = annotations[ROUTE] ?: return
        val routeType = routeAnnotation.getKClassArgument(Name.identifier("value"))?.fullyExpandedType(session) as? ConeClassLikeType ?: return
        val routeSymbol = session.symbolProvider.getClassLikeSymbolByClassId(routeType.lookupTag.classId) as? FirRegularClassSymbol ?: return

        fun error(message: String) {
            val path = context.containingFile?.path
            val text = path?.let { runCatching { File(it).readText() }.getOrNull() }
            val prefix = text?.take(declaration.source?.startOffset ?: 0)
            messages.report(
                CompilerMessageSeverity.ERROR, "Easy Navigation: $message",
                path?.let { CompilerMessageLocation.create(it, (prefix?.count { char -> char == '\n' } ?: 0) + 1, 1, null) },
            )
        }

        fun implementsRoute(symbol: FirRegularClassSymbol, visited: MutableSet<ClassId> = mutableSetOf()): Boolean {
            if (!visited.add(symbol.classId)) return false
            if (symbol.classId.asSingleFqName().asString() == "$CORE.model.NavigationRoute") return true
            return symbol.fir.superTypeRefs.any { ref ->
                val type = ref.coneType.fullyExpandedType(session) as? ConeClassLikeType ?: return@any false
                val parent = session.symbolProvider.getClassLikeSymbolByClassId(type.lookupTag.classId) as? FirRegularClassSymbol
                parent != null && implementsRoute(parent, visited)
            }
        }

        if (COMPOSABLE !in annotations) error("destination ${declaration.name} must be @Composable.")
        if (!implementsRoute(routeSymbol)) error("@Route value ${routeSymbol.classId.asSingleFqName()} must implement NavigationRoute.")
        val serializable = ClassId.topLevel(FqName("kotlinx.serialization.Serializable"))
        if (routeSymbol.fir.annotations.none { it.toAnnotationClassId(session) == serializable }) {
            error("@Route value ${routeSymbol.classId.asSingleFqName()} must be @Serializable.")
        }
        if (declaration.contextParameters.isNotEmpty()) error("destination ${declaration.name} cannot require context parameters.")
        val matching = declaration.valueParameters.filter {
            (it.returnTypeRef.coneType.fullyExpandedType(session) as? ConeClassLikeType)?.lookupTag?.classId == routeSymbol.classId
        }
        if (matching.size > 1) error("destination ${declaration.name} has multiple route parameters.")
        declaration.valueParameters.filter { it !in matching }.forEach {
            if (it.defaultValue == null && !it.isVararg) error("parameter '${it.name}' needs a default value; only the route is supplied automatically.")
        }
    }
}
