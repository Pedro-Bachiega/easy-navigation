package com.pedrobneto.easy.navigation.processor.library.model

@JvmInline
value class QualifiedName(val raw: String) {
    val packageName: String get() = raw.substringBeforeLast('.')
    val className: String get() = raw.substringAfterLast('.')
}
