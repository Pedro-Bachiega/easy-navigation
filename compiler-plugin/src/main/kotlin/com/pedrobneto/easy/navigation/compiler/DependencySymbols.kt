package com.pedrobneto.easy.navigation.compiler

import java.io.File
import java.util.zip.ZipFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/** Reads compiler metadata, including KLIB constants, without loading consumer classes. */
internal class DependencySymbols(files: List<File>) {
    val classes = mutableMapOf<String, Pair<String, String>>()
    val constants = mutableMapOf<String, Any>()
    val aliases = mutableMapOf<String, String>()

    init {
        files.distinct().filter(File::exists).forEach { file ->
            if (file.isDirectory) file.walkTopDown().filter { it.extension in setOf("knm", "class") }.forEach {
                read(it.invariantSeparatorsPath, it.readBytes())
            } else if (file.extension in setOf("jar", "klib", "zip")) {
                ZipFile(file).use { archive ->
                    archive.entries().asSequence().filter { it.name.endsWith(".knm") || it.name.endsWith(".class") }.forEach {
                        read(it.name, archive.getInputStream(it).use { stream -> stream.readBytes() })
                    }
                }
            }
        }
    }

    private fun read(path: String, bytes: ByteArray) {
        if (path.endsWith(".class")) readClass(bytes) else readFragment(bytes)
    }

    private fun readClass(bytes: ByteArray) {
        ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM9) {
            private var owner = ""
            private var packageName = ""
            private var metadataKind = 0
            private val fields = mutableMapOf<String, Any>()
            private val data1 = mutableListOf<String>()
            private val data2 = mutableListOf<String>()
            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                owner = name.replace('/', '.').replace('$', '.')
                packageName = name.substringBeforeLast('/', "").replace('/', '.')
                classes[owner] = packageName to name.substringAfterLast('/').substringAfterLast('$')
            }
            override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
                if (value != null && access and Opcodes.ACC_STATIC != 0) {
                    constants["$owner.$name"] = value
                    fields[name] = value
                }
                return null
            }
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                if (descriptor != "Lkotlin/Metadata;") return null
                return object : AnnotationVisitor(Opcodes.ASM9) {
                    override fun visit(name: String, value: Any) {
                        if (name == "k") metadataKind = value as Int
                    }
                    override fun visitArray(name: String): AnnotationVisitor? {
                        val values = when (name) { "d1" -> data1; "d2" -> data2; else -> return null }
                        return object : AnnotationVisitor(Opcodes.ASM9) {
                            override fun visit(name: String?, value: Any) { values.add(value as String) }
                        }
                    }
                }
            }
            override fun visitEnd() {
                if (metadataKind !in setOf(2, 5)) return
                fields.forEach { (name, value) -> constants["$packageName.$name".trimStart('.')] = value }
                if (data1.isEmpty()) return
                val (names, proto) = JvmProtoBufUtil.readPackageDataFrom(data1.toTypedArray(), data2.toTypedArray())
                proto.typeAliasList.forEach { alias ->
                    val type = if (alias.hasExpandedType()) alias.expandedType else proto.typeTable.getType(alias.expandedTypeId)
                    if (type.hasClassName()) aliases["$packageName.${names.getString(alias.name)}".trimStart('.')] = names.getQualifiedClassName(type.className).replace('/', '.')
                }
            }
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    }

    private fun readFragment(bytes: ByteArray) {
        val fragment = parsePackageFragment(bytes)
        val names = NameResolverImpl(fragment.strings, fragment.qualifiedNames)
        val packageName = fragment.getExtension(KlibMetadataProtoBuf.fqName)
        fragment.class_List.forEach { declaration ->
            val qualified = names.getQualifiedClassName(declaration.fqName).replace('/', '.')
            classes[qualified] = packageName to qualified.substringAfterLast('.')
            declaration.propertyList.forEach { readConstant("$qualified.${names.getString(it.name)}", it, names) }
        }
        fragment.`package`.propertyList.forEach { readConstant("$packageName.${names.getString(it.name)}".trimStart('.'), it, names) }
        fragment.`package`.typeAliasList.forEach { alias ->
            val type = if (alias.hasExpandedType()) alias.expandedType else fragment.`package`.typeTable.getType(alias.expandedTypeId)
            if (type.hasClassName()) aliases["$packageName.${names.getString(alias.name)}".trimStart('.')] = names.getQualifiedClassName(type.className).replace('/', '.')
        }
    }

    private fun readConstant(name: String, property: ProtoBuf.Property, names: NameResolverImpl) {
        if (!property.hasExtension(KlibMetadataProtoBuf.compileTimeValue)) return
        val value = property.getExtension(KlibMetadataProtoBuf.compileTimeValue)
        when (value.type) {
            ProtoBuf.Annotation.Argument.Value.Type.STRING -> constants[name] = names.getString(value.stringValue)
            ProtoBuf.Annotation.Argument.Value.Type.FLOAT -> constants[name] = value.floatValue
            ProtoBuf.Annotation.Argument.Value.Type.DOUBLE -> constants[name] = value.doubleValue
            ProtoBuf.Annotation.Argument.Value.Type.INT, ProtoBuf.Annotation.Argument.Value.Type.LONG -> constants[name] = value.intValue
            else -> Unit
        }
    }
}
