package pl.touk.exposed.generator.model

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import pl.touk.exposed.Convert
import pl.touk.exposed.Converter
import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.toTypeElement
import pl.touk.exposed.generator.validation.TypeConverterNotSupportedException
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.persistence.Column
import javax.persistence.GeneratedValue

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
        val typeMirror = columnElt.asType()
        val type = if (converter != null) PropertyType.TYPE_WRAPPER else columnElt.asType().getTypeDefinition()
        val columnDefinition = PropertyDefinition(name = name, columnName = columnName, annotation = columnAnn,
                type = type, typeMirror = typeMirror, nullable = isNullable(columnElt), converter = converter)
        entity.addProperty(columnDefinition)
    }

    private fun processIds(graphs: EntityGraphs) =
            processElements(annEnv.ids, graphs) { entity, idElt ->
                val type = idElt.asType().getIdTypeDefinition()
                val columnAnn: Column? = idElt.getAnnotation(Column::class.java)
                val columnName = getColumnName(columnAnn, idElt)
                val genValAnn : GeneratedValue? = idElt.getAnnotation(GeneratedValue::class.java)
                val generatedValue = genValAnn?.let { true } ?: false

                val idDef = IdDefinition(
                        name = idElt.simpleName, columnName = columnName, type = type,
                        annotation = columnAnn, typeMirror = idElt.asType(), generatedValue = generatedValue
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
            val converterTypeArguments = converterType.toTypeElement()
                    .interfaces
                    .find { it.asDeclaredType().asElement().toTypeElement().qualifiedName.asVariable() == Converter::class.qualifiedName }
                    ?.asDeclaredType()
                    ?.typeArguments

            if (converterTypeArguments?.size != 2) return null //TODO handle

            val databaseType = converterTypeArguments[1]
            val typeWrapper = databaseType.getTypeDefinition().typeWrapper() ?: throw TypeConverterNotSupportedException(databaseType.getTypeDefinition())

            return ConverterDefinition(name = converterType.qualifiedName.asVariable(), typeWrapper = typeWrapper)
        }
    }

    private fun TypeMirror.getTypeDefinition(): PropertyType {
        return when {
            isString() -> PropertyType.STRING
            isBoolean() -> PropertyType.BOOL
            isLong() -> PropertyType.LONG
            isInteger() -> PropertyType.INTEGER
            isShort() -> PropertyType.SHORT
            isFloat() -> PropertyType.FLOAT
            isDouble() -> PropertyType.DOUBLE
            isBigDecimal() -> PropertyType.BIG_DECIMAL
            isUUID() -> PropertyType.UUID
            isDateTime() -> PropertyType.DATE_TIME
            else -> TODO()
        }
    }

    private fun TypeMirror.getIdTypeDefinition(): IdType {
        return when {
            isUUID() -> IdType.UUID
            isString() -> IdType.STRING
            isInteger() -> IdType.INTEGER
            isShort() -> IdType.SHORT
            isNumeric() -> IdType.LONG
            else -> TODO()
        }
    }

    private fun TypeMirror.isString() = typeEnv.isSameType(this, "java.lang.String")

    private fun TypeMirror.isLong() = typeEnv.isSameType(this, "java.lang.Long") || kind == TypeKind.LONG

    private fun TypeMirror.isInteger() = typeEnv.isSameType(this, "java.lang.Integer") || kind == TypeKind.INT

    private fun TypeMirror.isShort() = typeEnv.isSameType(this, "java.lang.Short") || kind == TypeKind.SHORT

    private fun TypeMirror.isFloat() = typeEnv.isSameType(this, "java.lang.Float") || kind == TypeKind.FLOAT

    private fun TypeMirror.isDouble() = typeEnv.isSameType(this, "java.lang.Double") || kind == TypeKind.DOUBLE

    private fun TypeMirror.isBigDecimal() = typeEnv.isSameType(this, "java.math.BigDecimal")

    private fun TypeMirror.isBoolean() = typeEnv.isSameType(this, "java.lang.Boolean") || kind == TypeKind.BOOLEAN

    private fun TypeMirror.isUUID() = typeEnv.isSameType(this, "java.util.UUID")

    private fun TypeMirror.isDateTime() = typeEnv.isSameType(this, "org.joda.time.DateTime")

    // TODO float/int/long/double
    private fun TypeMirror.isNumeric() = typeEnv.isSubType(this, "java.lang.Number") ||
            kind in listOf(TypeKind.LONG, TypeKind.INT, TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.SHORT)

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

}
