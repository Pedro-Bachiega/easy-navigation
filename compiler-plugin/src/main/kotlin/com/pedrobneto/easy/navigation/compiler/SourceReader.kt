package com.pedrobneto.easy.navigation.compiler

import java.io.File
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/** Syntax discovery deliberately precedes FIR: consumers can already import not-yet-generated registries. */
internal class SourceReader {
    fun read(sources: Map<String, List<File>>, graph: SourceGraph): List<Destination> {
        val disposable = Disposer.newDisposable()
        try {
            val environment = KotlinCoreEnvironment.createForProduction(
                disposable, CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
            val factory = KtPsiFactory(environment.project, false)
            val files = sources.flatMap { (sourceSet, paths) ->
                paths.distinct().sortedBy(File::getPath).map { path ->
                    SourceFile(sourceSet, path, factory.createFile(path.name, path.readText()))
                }
            }
            return files.flatMap { source ->
                val visible = graph.ancestors(source.sourceSet)
                val resolver = Names(source.file, files.filter { it.sourceSet in visible }.map { it.file })
                source.file.declarations.filterIsInstance<KtNamedFunction>().mapNotNull { function ->
                    val annotations = function.annotationEntries.map { resolver.annotation(it) }
                    if (annotations.none { it.name in DESTINATION_ANNOTATIONS }) return@mapNotNull null
                    val location = "${source.path}:${source.file.text.take(function.textOffset).count { it == '\n' } + 1}"
                    fun requireValid(condition: Boolean, message: String) {
                        require(condition) { "$location: Easy Navigation: $message" }
                    }
                    fun first(name: String) = annotations.firstOrNull { it.name == name }
                    requireValid(first(COMPOSABLE) != null, "destination ${function.name} must be @Composable.")
                    val route = first(ROUTE)?.type("value")
                    requireValid(route != null, "destination ${function.name} must declare @Route(...).")
                    requireValid(!function.hasModifier(KtTokens.PRIVATE_KEYWORD), "destination must be visible outside its file.")
                    requireValid(!function.hasModifier(KtTokens.SUSPEND_KEYWORD), "destination must not be suspend.")
                    requireValid(function.receiverTypeReference == null && function.typeParameters.isEmpty(), "destination must not require a receiver or type arguments.")
                    val routeParameters = function.valueParameters.filter {
                        resolver.type(it.typeReference?.text.orEmpty().removeSuffix("?")) == route
                    }
                    requireValid(routeParameters.size <= 1, "destination has multiple route parameters.")
                    function.valueParameters.filter { it !in routeParameters }.forEach {
                        requireValid(it.hasDefaultValue() || it.isVarArg, "parameter '${it.name}' needs a default value; only the route can be supplied automatically.")
                    }
                    val extras = annotations.filter { it.name == EXTRA }.map {
                        (it.type("host") ?: error("$location: @ExtraPane needs a host")) to (it.float("ratio") ?: .5f)
                    }
                    val strategies = listOf(first(ADAPTIVE) != null, first(SINGLE) != null, extras.isNotEmpty()).count { it }
                    requireValid(strategies <= 1, "use only one pane strategy.")
                    val modal = first(MODAL) != null
                    requireValid(!modal || strategies == 0, "@Modal cannot be combined with a pane annotation.")
                    val routeName = resolver.className(route!!)
                    Destination(
                        source.sourceSet, location, route, resolver.packageName(route), routeName,
                        listOf(source.file.packageFqName.asString(), function.name!!).filter(String::isNotEmpty).joinToString("."),
                        routeParameters.singleOrNull()?.name,
                        annotations.filter { it.name == SCOPE }.map { it.string("value") },
                        first(GLOBAL) != null,
                        annotations.filter { it.name == DEEPLINK }.map { it.string("value") },
                        first(PARENT_ROUTE)?.type("value"), first(PARENT_DEEPLINK)?.string("value"),
                        when {
                            extras.isNotEmpty() -> Pane.Extra(extras)
                            first(SINGLE) != null -> Pane.Single
                            else -> Pane.Adaptive(first(ADAPTIVE)?.float("ratio") ?: 1f)
                        }, modal,
                    )
                }
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private data class SourceFile(val sourceSet: String, val path: File, val file: KtFile)
}

internal const val COMPOSABLE = "androidx.compose.runtime.Composable"
internal const val ROUTE = "$CORE.annotation.Route"
internal const val SCOPE = "$CORE.annotation.Scope"
internal const val GLOBAL = "$CORE.annotation.GlobalScope"
internal const val DEEPLINK = "$CORE.annotation.Deeplink"
internal const val PARENT_ROUTE = "$CORE.annotation.ParentRoute"
internal const val PARENT_DEEPLINK = "$CORE.annotation.ParentDeeplink"
internal const val ADAPTIVE = "$CORE.adaptive.AdaptivePane"
internal const val SINGLE = "$CORE.adaptive.SinglePane"
internal const val EXTRA = "$CORE.adaptive.ExtraPane"
internal const val MODAL = "$CORE.modal.Modal"
internal val DESTINATION_ANNOTATIONS = setOf(ROUTE, DEEPLINK, PARENT_ROUTE, PARENT_DEEPLINK)
private val KNOWN_ANNOTATIONS = DESTINATION_ANNOTATIONS + setOf(COMPOSABLE, SCOPE, GLOBAL, ADAPTIVE, SINGLE, EXTRA, MODAL)

private class Names(private val file: KtFile, private val visible: List<KtFile>) {
    private val imports = file.importDirectives.filterNot { it.isAllUnder }.associate {
        (it.aliasName ?: it.importedFqName?.shortName()?.asString().orEmpty()) to it.importedFqName?.asString().orEmpty()
    }
    private val stars = file.importDirectives.filter { it.isAllUnder }.mapNotNull { it.importedFqName?.asString() }
    private val classes = mutableMapOf<String, Pair<String, String>>()
    private val aliases = mutableMapOf<String, Pair<KtFile, String>>()
    private val constants = mutableMapOf<String, Pair<KtFile, KtExpression>>()

    init {
        fun collect(owner: KtFile, declarations: List<KtDeclaration>, prefix: String = "") {
            declarations.forEach { declaration ->
                val name = (declaration as? KtNamedDeclaration)?.name ?: return@forEach
                val relative = if (prefix.isEmpty()) name else "$prefix.$name"
                val qualified = listOf(owner.packageFqName.asString(), relative).filter(String::isNotEmpty).joinToString(".")
                if (declaration is KtClassOrObject) {
                    classes[qualified] = owner.packageFqName.asString() to name
                    collect(owner, declaration.declarations, relative)
                }
                if (declaration is KtTypeAlias) aliases[qualified] = owner to declaration.getTypeReference()!!.text
                if (declaration is KtProperty && declaration.hasModifier(KtTokens.CONST_KEYWORD)) {
                    declaration.initializer?.let { constants[qualified] = owner to it }
                }
            }
        }
        visible.forEach { collect(it, it.declarations) }
    }

    private fun resolve(text: String): String {
        val cleaned = text.replace("`", "").trim()
        val head = cleaned.substringBefore('.')
        imports[head]?.let { return it + cleaned.removePrefix(head) }
        val local = listOf(file.packageFqName.asString(), cleaned).filter(String::isNotEmpty).joinToString(".")
        if (local in classes || local in aliases || local in constants) return local
        val candidates = stars.map { "$it.$cleaned" }.filter { it in classes || it in aliases || it in constants || it in KNOWN_ANNOTATIONS }
        require(candidates.size <= 1) { "Ambiguous name '$text' in ${file.name}; use an explicit import." }
        candidates.singleOrNull()?.let { return it }
        if ('.' in cleaned && head.firstOrNull()?.isLowerCase() == true) return cleaned
        // External symbols are resolved by FIR in the normal compilation. A single wildcard is unambiguous here.
        if (stars.size == 1) return "${stars.single()}.$cleaned"
        return local
    }

    fun type(text: String): String {
        val name = resolve(text)
        val alias = aliases[name] ?: return name
        // Alias bodies use their own imports, not the caller's imports.
        return Names(alias.first, visible).type(alias.second)
    }

    fun className(name: String): String = classes[name]?.second ?: name.substringAfterLast('.')
    fun packageName(name: String): String = classes[name]?.first ?: name.split('.').takeWhile { it.firstOrNull()?.isLowerCase() == true }.joinToString(".")

    fun annotation(entry: KtAnnotationEntry): Annotation = Annotation(type(entry.typeReference!!.text), entry, this)

    fun value(expression: KtExpression, seen: Set<String> = emptySet()): Any {
        return when (expression) {
            is KtStringTemplateExpression -> expression.entries.joinToString("") { entry ->
                when (entry) {
                    is KtLiteralStringTemplateEntry -> entry.text
                    is KtEscapeStringTemplateEntry -> entry.unescapedValue
                    is KtStringTemplateEntryWithExpression -> value(entry.expression!!, seen).toString()
                    else -> error("Unsupported string annotation expression: ${expression.text}")
                }
            }
            is KtConstantExpression -> expression.text.removeSuffix("f").removeSuffix("F").toFloatOrNull()
                ?: error("Unsupported annotation constant: ${expression.text}")
            is KtPrefixExpression -> -((value(expression.baseExpression!!, seen) as Number).toFloat())
            is KtParenthesizedExpression -> value(expression.expression!!, seen)
            is KtBinaryExpression -> {
                require(expression.operationToken == KtTokens.PLUS) { "Unsupported annotation expression: ${expression.text}" }
                val left = value(expression.left!!, seen)
                val right = value(expression.right!!, seen)
                if (left is String || right is String) left.toString() + right.toString() else (left as Number).toFloat() + (right as Number).toFloat()
            }
            else -> {
                val name = resolve(expression.text)
                require(name !in seen) { "Cyclic annotation constant: $name" }
                val constant = constants[name] ?: error("Cannot read annotation constant '$name'; use a source constant or literal.")
                Names(constant.first, visible).value(constant.second, seen + name)
            }
        }
    }
}

private class Annotation(val name: String, private val entry: KtAnnotationEntry, private val names: Names) {
    private fun expression(parameter: String): KtExpression? = entry.valueArguments.firstOrNull {
        it.getArgumentName()?.asName?.asString() == parameter
    }?.getArgumentExpression() ?: entry.valueArguments.firstOrNull {
        !it.isNamed() && parameter in setOf("value", "host", "ratio")
    }?.getArgumentExpression()

    fun type(parameter: String): String? = (expression(parameter) as? KtClassLiteralExpression)?.receiverExpression?.text?.let(names::type)
    fun string(parameter: String): String = names.value(expression(parameter) ?: error("Missing $parameter in ${entry.text}")) as String
    fun float(parameter: String): Float? {
        val argument = if (parameter == "ratio" && name == EXTRA) {
            entry.valueArguments.firstOrNull { it.getArgumentName()?.asName?.asString() == parameter }?.getArgumentExpression()
                ?: entry.valueArguments.filterNot { it.isNamed() }.getOrNull(1)?.getArgumentExpression()
        } else expression(parameter)
        return argument?.let { (names.value(it) as Number).toFloat() }
    }
}
