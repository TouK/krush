package pl.touk.krush.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import pl.touk.krush.model.Type
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

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
