package pl.touk.krush.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.KmClassifier
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import pl.touk.krush.model.Type
import javax.lang.model.element.VariableElement

private fun List<String>.packageName() = this.dropLast(1).joinToString(separator = ".")

@KotlinPoetMetadataPreview
fun ImmutableKmClass.toClassName(): ClassName {
    return this.name.split("/").let { ClassName(it.packageName(), it.last()) }
}

@KotlinPoetMetadataPreview
fun ImmutableKmType.toModelType(): Type {
    return when(val classifier = (this.abbreviatedType?.classifier ?: this.classifier)){
        is KmClassifier.Class -> {
            classifier.name
                .split("/").let { Type(it.packageName(), it.last()) }
        }
        is KmClassifier.TypeAlias -> classifier.name
            .split("/").let { Type(it.packageName(), it.last(), this.copy(abbreviatedType = null).toModelType()) }
        is KmClassifier.TypeParameter -> TODO()
    }
}

fun VariableElement.isNullable() =
    this.getAnnotation(NotNull::class.java) == null && this.getAnnotation(Nullable::class.java) != null
