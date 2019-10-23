package pl.touk.exposed.generator.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.KmClassifier
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import pl.touk.exposed.Convert
import pl.touk.exposed.Converter
import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.toTypeElement
import pl.touk.exposed.generator.validation.ElementTypeNotFoundException
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.persistence.Column
import javax.persistence.GeneratedValue

@KotlinPoetMetadataPreview
class ColumnProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) {
        processIds(graphs)
        processColumns(graphs)
    }

    private fun processColumns(graphs: EntityGraphs) =
            processElements(annEnv.columns, graphs) { entity, columnElt ->
        val columnAnn: Column? = columnElt.getAnnotation(Column::class.java)
        val name = columnElt.simpleName
        val columnName = getColumnName(columnAnn, columnElt)
        val converter = getConverterDefinition(columnElt)
        val type = columnElt.toType() ?: throw ElementTypeNotFoundException(columnElt)
        val columnDefinition = PropertyDefinition(name = name, columnName = columnName, annotation = columnAnn,
                type = type, nullable = isNullable(columnElt), converter = converter)
        entity.addProperty(columnDefinition)
    }

    private fun processIds(graphs: EntityGraphs) =
            processElements(annEnv.ids, graphs) { entity, idElt ->
                val columnAnn: Column? = idElt.getAnnotation(Column::class.java)
                val columnName = getColumnName(columnAnn, idElt)
                val converter = getConverterDefinition(idElt)
                val genValAnn : GeneratedValue? = idElt.getAnnotation(GeneratedValue::class.java)
                val generatedValue = genValAnn?.let { true } ?: false
                val type = idElt.toType() ?: throw ElementTypeNotFoundException(idElt)

                val idDef = IdDefinition(
                        name = idElt.simpleName, columnName = columnName, converter = converter,
                        annotation = columnAnn, type = type, generatedValue = generatedValue
                )
                entity.copy(id = idDef)
            }

    private fun isNullable(columnElt: VariableElement) =
            columnElt.getAnnotation(NotNull::class.java) == null && columnElt.getAnnotation(Nullable::class.java) != null

    private fun getColumnName(columnAnn: Column?, columnElt: VariableElement) =
            if (columnAnn == null || columnAnn.name.isEmpty()) columnElt.simpleName else typeEnv.elementUtils.getName(columnAnn.name)

    private fun getConverterDefinition(columnElt: VariableElement): ConverterDefinition? {
        val converterType = columnElt.annotationMirror(Convert::class.java.canonicalName)?.valueType("value")

        return converterType?.let {
            val databaseType = (converterType.toTypeElement().toImmutableKmClass().functions
                    .find { it.name == Converter<*,*>::convertToDatabaseColumn.name }
                    ?.returnType?.classifier as KmClassifier.Class?)
                    ?.name?.split("/")
                    ?.let { Type(it.dropLast(1).joinToString(separator = "."), it.last()) } ?: throw IllegalStateException()

            return ConverterDefinition(name = converterType.qualifiedName.asVariable(), targetType = databaseType)
        }
    }

    private fun VariableElement.annotationMirror(className: String): AnnotationMirror? {
        for (mirror in this.annotationMirrors) {
            if (typeEnv.isSameType(mirror.annotationType, className)) {
                return mirror
            }
        }
        return null
    }

    private fun AnnotationMirror.valueType(valueName: String): TypeElement? {
        val annotationValue = this.value(valueName) ?: return null

        val value = annotationValue.value as TypeMirror
        return typeEnv.typeUtils.asElement(value) as TypeElement
    }

    private fun AnnotationMirror.value(valueName: String): AnnotationValue? {
        for (entry in this.elementValues.entries) {
            if (entry.key.simpleName.toString() == valueName) {
                return entry.value
            }
        }
        return null
    }

    private fun VariableElement.toType(): Type? {
        val classifierName = (this.enclosingElement.toTypeElement().toImmutableKmClass().properties
                .find { it.name == this.simpleName.toString() }
                ?.returnType
                ?.classifier as KmClassifier.Class?)
                ?.name

        return classifierName?.split("/")?.let { Type(it.dropLast(1).joinToString(separator = "."), it.last()) }
    }
}
