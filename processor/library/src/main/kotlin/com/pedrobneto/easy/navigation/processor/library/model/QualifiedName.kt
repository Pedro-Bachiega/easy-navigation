package com.pedrobneto.easy.navigation.processor.library.model

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.reflect.KClass

@JvmInline
value class QualifiedName(val raw: String) {
    val packageName: String get() = raw.substringBeforeLast('.')
    val className: String get() = raw.substringAfterLast('.')

    companion object {
        operator fun invoke(kClass: () -> KClass<*>): QualifiedName? =
            runCatching { kClass().qualifiedName.orEmpty() }
                .getOrElse { exception ->
                    "(.*ClassNotFoundException: )([a-zA-Z._]+)".toRegex()
                        .find(exception.message.orEmpty())
                        ?.groupValues
                        ?.last()
                }.takeIf { it != NavigationRoute::class.qualifiedName }
                ?.let(::QualifiedName)
    }
}