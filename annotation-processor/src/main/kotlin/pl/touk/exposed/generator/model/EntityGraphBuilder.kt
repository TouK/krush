package pl.touk.exposed.generator.model

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import pl.touk.exposed.Convert
import pl.touk.exposed.Converter
import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.enclosingTypeElement
import pl.touk.exposed.generator.env.toTypeElement
import pl.touk.exposed.generator.env.toVariableElement
import pl.touk.exposed.generator.validation.EntityNotMappedException
import pl.touk.exposed.generator.validation.GeneratedValueWithoutIdException
import pl.touk.exposed.generator.validation.TypeConverterNotSupportedException
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import javax.persistence.OneToOne

class EntityGraphBuilder(
        private val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment
) {

    fun build(): EntityGraphs {
        val graphs = EntityGraphs()

        // TODO split
        for (entityElt in annEnv.entities) {
            val graph = graphs.getOrDefault(entityElt.packageName, EntityGraph())
            graph[entityElt] = EntityDefinition(
                    name = entityElt.simpleName, qualifiedName = entityElt.qualifiedName, table = entityElt.tableName
            )
            graphs[entityElt.packageName] = graph
        }

        for (idElt in annEnv.ids) {
            val entityType = idElt.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            graph.computeIfPresent(entityType) { _, entity ->

                val type = idElt.asType().getIdTypeDefinition()
                val columnAnn: Column? = idElt.getAnnotation(Column::class.java)
                val columnName = getColumnName(columnAnn, idElt)

                entity.copy(id = IdDefinition(name = idElt.simpleName, columnName = columnName, type = type,
                        annotation = columnAnn, typeMirror = idElt.asType()))
            }
        }

        for (genValueElt in annEnv.genValues) {
            val entityType = genValueElt.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            graph.computeIfPresent(entityType) { _, entity ->
                val idDefinition = entity.id ?: throw GeneratedValueWithoutIdException(genValueElt, entityType)
                entity.copy(id = idDefinition.copy(generatedValue = true))
            }
        }

        for (columnElt in annEnv.columns) {
            val entityType = columnElt.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            val converter = getConverterDefinition(columnElt)

            graph.computeIfPresent(entityType) { _, entity ->
                val columnAnn: Column? = columnElt.getAnnotation(Column::class.java)
                val name = columnElt.simpleName
                val columnName = getColumnName(columnAnn, columnElt)
                val typeMirror = columnElt.asType()
                val type = if (converter != null) PropertyType.TYPE_WRAPPER else columnElt.asType().getTypeDefinition()
                val columnDefinition = PropertyDefinition(name = name, columnName = columnName, annotation = columnAnn,
                        type = type, typeMirror = typeMirror, nullable = isNullable(columnElt), converter = converter)
                entity.addProperty(columnDefinition)
            }
        }

        for (oneToOne in annEnv.oneToOne) {
            val entityType = oneToOne.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)

            graph.computeIfPresent(entityType) { _, entity ->
                val join: JoinColumn? = oneToOne.getAnnotation(JoinColumn::class.java)
                val target = oneToOne.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
                val parentEntityId = graphs.entityId(target)
                val mappedBy: String? = oneToOne.getAnnotation(OneToOne::class.java)?.mappedBy?.ifBlank { null }

                val associationDef = AssociationDefinition(
                        name = oneToOne.simpleName, type = AssociationType.ONE_TO_ONE, mapped = mappedBy.isNullOrEmpty(),
                        mappedBy = mappedBy, target = target, joinColumn = join?.name, targetId = parentEntityId
                )
                entity.addAssociation(associationDef)
            }
        }

        for (oneToMany in annEnv.oneToMany) {
            val entityType = oneToMany.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            val otmAnn = oneToMany.getAnnotation(OneToMany::class.java)
            val target = oneToMany.asType().getTypeArgument().asElement().toTypeElement()

            graph.computeIfPresent(entityType) { _, entity ->
                val parentEntityId = graphs.entityId(target)
                val associationDef = AssociationDefinition(
                        name = oneToMany.simpleName, type = AssociationType.ONE_TO_MANY,
                        target = target, mappedBy = otmAnn.mappedBy, targetId = parentEntityId
                )
                entity.addAssociation(associationDef)
            }
        }

        for (manyToOne in annEnv.manyToOne) {
            val entityType = manyToOne.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)

            graph.computeIfPresent(entityType) { _, entity ->
                val join = manyToOne.getAnnotation(JoinColumn::class.java)
                val target = manyToOne.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
                val parentEntityId = graphs.entityId(target)
                val associationDef = AssociationDefinition(
                        name = manyToOne.simpleName, type = AssociationType.MANY_TO_ONE,
                        target = target, joinColumn = join.name, targetId = parentEntityId
                )
                entity.addAssociation(associationDef)
            }
        }

        for (manyToMany in annEnv.manyToMany) {
            val entityType = manyToMany.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            val joinTableAnn = manyToMany.getAnnotation(JoinTable::class.java)
            val target = manyToMany.asType().getTypeArgument().asElement().toTypeElement()

            graph.computeIfPresent(entityType) { _, entity ->
                val parentEntityId = graphs.entityId(entityType)
                val associationDef = AssociationDefinition(
                        name = manyToMany.simpleName, type = AssociationType.MANY_TO_MANY,
                        target = target, joinTable = joinTableAnn.name, targetId = parentEntityId
                )
                entity.addAssociation(associationDef)
            }
        }

        // unidirectional post-process
        for (oneToMany in annEnv.oneToMany) {
            val entityType = oneToMany.enclosingTypeElement()
            val joinColumnAnn = oneToMany.getAnnotation(JoinColumn::class.java) ?: continue
            val targetType = oneToMany.asType().getTypeArgument().asElement().toTypeElement()

            val graph = graphs[targetType.packageName] ?: throw EntityNotMappedException(targetType)
            graph.computeIfPresent(targetType) { _, entity ->
                val isMapped = entity.associations.any { assoc -> assoc.type == AssociationType.MANY_TO_ONE && assoc.target == entityType }
                if (isMapped) {
                    entity
                } else {
                    val parentEntityId = graphs.entityId(entityType)
                    val associationDef = AssociationDefinition(
                            name = entityType.simpleName, type = AssociationType.MANY_TO_ONE,
                            target = entityType, joinColumn = joinColumnAnn.name, mapped = false,
                            targetId = parentEntityId
                    )
                    entity.addAssociation(associationDef)
                }
            }
        }
        return graphs
    }

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

    private fun isNullable(columnElt: VariableElement) =
            columnElt.getAnnotation(NotNull::class.java) == null && columnElt.getAnnotation(Nullable::class.java) != null

    private fun TypeMirror.asDeclaredType(): DeclaredType {
        require(this is DeclaredType)
        return this
    }

    private fun TypeMirror.getTypeArgument(): DeclaredType {
        return this.asDeclaredType().typeArguments[0].asDeclaredType()
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

    private fun PropertyType.typeWrapper(): TypeWrapperTargetType? {
        return when(this) {
            PropertyType.STRING -> TypeWrapperTargetType.STRING
            PropertyType.LONG ->  TypeWrapperTargetType.LONG
            else -> null
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

    private fun TypeMirror.isNumeric() = typeEnv.isSubType(this, Number::class.java.canonicalName) ||
            kind in listOf(TypeKind.LONG, TypeKind.INT, TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.SHORT)
}
