package pl.touk.krush.poet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.KmClassifier
import pl.touk.krush.model.Type

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
