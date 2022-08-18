package pl.touk.krush.source

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import org.jetbrains.exposed.sql.ResultRow
import pl.touk.krush.RowWrapper
import pl.touk.krush.meta.toClassName
import pl.touk.krush.model.AssociationType
import pl.touk.krush.model.EntityDefinition
import pl.touk.krush.model.Type
import pl.touk.krush.model.asVariable
import pl.touk.krush.model.capitalize

fun buildToEntityFuncSelf(entityType: Type, entity: EntityDefinition): FunSpec {
    val func = FunSpec.builder("to${entity.name}")
        .receiver(RowWrapper::class.java)
        .returns(entityType.toClassName())
        .addParameter(
            "alias",
            generateSelfAlias(entityType)
        )
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                generateSelfAlias(entityType)
                    .copy(nullable = true)
            ).defaultValue("null")
                .build()
        )

    val idMapping = generateIdMapping(entity)
    val propsMappings = generatePropsMapping(entity)
    val embeddedMappings = generateEmbeddedMappings(entity)
    val associationsMappings = generateAssociationMappings(entity)

    // Add empty but mutable lists for O2M and M2M connections, so that the relations can be filled in later
    // without possibly breaking existing references to this object
    val listAssociationMapping = entity.getAssociations(AssociationType.ONE_TO_MANY, AssociationType.MANY_TO_MANY)
        .map { "\t${it.name} = mutableListOf()" }

    val mapping =
        (idMapping + propsMappings + embeddedMappings + associationsMappings + listAssociationMapping).joinToString(",\n")

    func.addStatement("return %T(\n$mapping\n)", entityType.toClassName())

    return func.build()
}

private fun generateAssociationMappings(entity: EntityDefinition) =
    entity.getAssociations(AssociationType.MANY_TO_ONE, AssociationType.ONE_TO_ONE)
        .filter { assoc -> assoc.mapped }
        .map {
            val name = it.name
            val targetName = it.target.simpleName
            if (!it.nullable) {
                "\t$name = this.to$targetName()"
            } else {
                if (entity.hasSelfReferentialAssoc()) {
                    "\t$name = this.row[${entity.name}Table.${name}Id]?.let { nextAlias?.let { this.to$targetName(nextAlias, null) } }"
                } else {
                    "\t$name = this[${entity.name}Table.${name}Id]?.let { this.to$targetName() }"
                }
            }
        }

private fun generateEmbeddedMappings(entity: EntityDefinition) =
    entity.embeddables.map { embeddable ->
        val embeddableName = embeddable.propertyName.asVariable()
        val embeddableMapping = embeddable.getPropertyNames().joinToString(", \n") { name ->
            val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
            "\t\t$name = this.row[alias${entity.name}Table.${tablePropName}Id]"
        }

        "\t$embeddableName = ${embeddable.qualifiedName}(\n$embeddableMapping\n\t)"
    }

private fun generatePropsMapping(entity: EntityDefinition) =
    entity.getPropertyNames().map { name ->
        "\t$name = this.row[alias[${entity.name}Table.${name}]]"
    }

private fun generateIdMapping(entity: EntityDefinition) = entity.id?.let { id ->
    if (id.embedded) {
        val embeddableIdName = id.name.asVariable()
        val embeddableIdMapping = id.properties.joinToString(", \n") { property ->
            val name = property.name
            "\t\t$name = this.row[alias[${entity.tableName}.${id.propName(property)}]]"
        }
        "\t$embeddableIdName = ${id.qualifiedName}(\n$embeddableIdMapping\n\t)"
    } else {
        "\t${id.name} = this.row[alias[${entity.tableName}.${id.name}]]"
    }
}?.let { listOf(it) } ?: emptyList()


private const val EXPOSED_PACKAGE_NAME = "org.jetbrains.exposed.sql"

fun buildSelfReferencesToMapFuncBuilder(entity: EntityDefinition, rootKey: TypeName, entityClass: ClassName) =
    FunSpec.builder("to${entity.name}Map")
        .receiver(Iterable::class.parameterizedBy(ResultRow::class))
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                ClassName(EXPOSED_PACKAGE_NAME, "Alias")
                    .parameterizedBy(ClassName(entityClass.packageName, "${entityClass}Table"))
                    .copy(nullable = true)
            ).defaultValue("null").build()
        )
        .returns(
            ClassName("kotlin.collections", "Map").parameterizedBy(rootKey, entityClass)
        )

fun buildSelfReferencesToEntityBuilder(entity: EntityDefinition, entityClass: ClassName) =
    FunSpec.builder("to${entity.name}")
        .receiver(RowWrapper::class.java)
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                ClassName(EXPOSED_PACKAGE_NAME, "Alias")
                    .parameterizedBy(ClassName(entityClass.packageName, "${entityClass}Table"))
                    .copy(nullable = true)
            ).defaultValue("null").build()
        )
        .returns(entityClass)

fun buildSelfReferencesToEntityListFunc(entityType: Type, entity: EntityDefinition): FunSpec {
    val entityName = entity.name

    val func = FunSpec.builder("to${entityName}List")
        .receiver(Iterable::class.parameterizedBy(ResultRow::class))
        .returns(List::class.asClassName().parameterizedBy(entityType.toClassName()))
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                generateSelfAlias(entityType)
                    .copy(nullable = true)
            ).defaultValue("null").build()
        )

    func.addStatement("return this.to${entityName}Map(nextAlias).values.toList()")

    return func.build()
}

private fun generateSelfAlias(entityType: Type): ParameterizedTypeName {
    val className = entityType.toClassName()
    return ClassName(EXPOSED_PACKAGE_NAME, "Alias")
        .parameterizedBy(ClassName(entityType.packageName, "${className}Table"))
}
