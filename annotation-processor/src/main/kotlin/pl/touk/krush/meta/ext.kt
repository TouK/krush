package pl.touk.krush.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import pl.touk.krush.model.Type
import pl.touk.krush.model.asObject
import pl.touk.krush.model.asVariable
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.persistence.Table

private fun List<String>.packageName() = this.dropLast(1).joinToString(separator = ".")

fun Element.toTypeElement(): TypeElement {
    require(this is TypeElement) { "Invalid element type ${this.kind}, type expected" }
    return this
}

fun Element.toVariableElement(): VariableElement {
    require(this is VariableElement) { "Invalid element type ${this.kind}, var expected" }
    return this
}

private fun TypeMirror.asDeclaredType(): DeclaredType {
    require(this is DeclaredType)
    return this
}

fun VariableElement.toTypeElement() = asType().asDeclaredType().asElement().toTypeElement()

@KotlinPoetMetadataPreview
fun KmClass.toClassName(): ClassName {
    return this.name.split("/").let { ClassName(it.packageName(), it.last()) }
}

val TypeElement.packageName: String
    get() {
        val dotIdx = this.qualifiedName.lastIndexOf('.')
        if (dotIdx < 0) {
            return "default"
        }
        return this.qualifiedName.substring(0 until dotIdx)
    }

val TypeElement.tableName: String
    get() {
        return this.getAnnotation(Table::class.java)?.name ?: this.simpleName.asVariable()
    }

fun TypeElement.toModelType() = Type(
    this.packageName, this.simpleName.toString()
)

fun Type.toClassName() = ClassName(packageName, simpleName)

fun Name.asObject() = this.toString().asObject()
fun Name.asVariable() = this.toString().asVariable()

@KotlinPoetMetadataPreview
fun KmType.toModelType(): Type {
    return when(val classifier = (this.abbreviatedType?.classifier ?: this.classifier)) {
        is KmClassifier.Class -> {
            classifier.name
                .split("/").let { Type(it.packageName(), it.last()) }
        }
        is KmClassifier.TypeAlias -> {
            val noAbbreviatedType = KmType(flags).also {
                it.classifier = this.classifier
                it.abbreviatedType = null
            }
            classifier.name
                .split("/").let { Type(it.packageName(), it.last(), noAbbreviatedType.toModelType()) }
        }
        is KmClassifier.TypeParameter -> TODO()
    }
}

fun VariableElement.isNullable() =
    this.getAnnotation(NotNull::class.java) == null && this.getAnnotation(Nullable::class.java) != null
