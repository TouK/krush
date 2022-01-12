package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toKmClass
import org.jetbrains.exposed.sql.ResultRow
import pl.touk.krush.RowWrapper
import pl.touk.krush.meta.toClassName
import pl.touk.krush.model.*
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
fun buildToEntityFuncSelf(entityType: TypeElement, entity: EntityDefinition): FunSpec {
    val entityClass = entityType.toKmClass().toClassName()
    val func = FunSpec.builder("to${entity.name}")
        .receiver(RowWrapper::class.java)
        .addParameter(
            "alias",
            ClassName("org.jetbrains.exposed.sql", "Alias")
                .parameterizedBy(ClassName(entityType.packageName, "${entityClass}Table"))
        )
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                ClassName("org.jetbrains.exposed.sql", "Alias")
                    .parameterizedBy(ClassName(entityType.packageName, "${entityClass}Table"))
                    .copy(nullable = true)
            ).defaultValue("null").build()
        )
        .returns(entityClass)

    val idMapping = entity.id?.let { id ->
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

    val propsMappings = entity.getPropertyNames().map { name ->
        "\t$name = this.row[alias[${entity.name}Table.${name}]]"
    }

    val embeddedMappings = entity.embeddables.map { embeddable ->
        val embeddableName = embeddable.propertyName.asVariable()
        val embeddableMapping = embeddable.getPropertyNames().joinToString(", \n") { name ->
            val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
            "\t\t$name = this.row[alias${entity.name}Table.${tablePropName}Id]"
        }

        "\t$embeddableName = ${embeddable.qualifiedName}(\n$embeddableMapping\n\t)"
    }

    val associationsMappings = entity.getAssociations(AssociationType.MANY_TO_ONE, AssociationType.ONE_TO_ONE)
        .filter { assoc -> assoc.mapped }
        .map {
            if (!it.nullable) {
                "\t${it.name} = this.to${it.target.simpleName}()"
            } else {
                if (entity.hasSelfReferentialAssoc()) {
                    "\t${it.name} = this.row[${entity.name}Table.${it.name}Id]?.let { nextAlias?.let { this.to${it.target.simpleName}(nextAlias, null) } }"
                } else {
                    "\t${it.name} = this[${entity.name}Table.${it.name}Id]?.let { this.to${it.target.simpleName}() }"
                }
            }
        }

    // Add empty but mutable lists for O2M and M2M connections, so that the relations can be filled in later
    // without possibly breaking existing references to this object
    val listAssociationMapping = entity.getAssociations(AssociationType.ONE_TO_MANY, AssociationType.MANY_TO_MANY)
        .map { "\t${it.name} = mutableListOf()" }

    val mapping =
        (idMapping + propsMappings + embeddedMappings + associationsMappings + listAssociationMapping).joinToString(",\n")

    func.addStatement("return %T(\n$mapping\n)", entityClass)

    return func.build()
}


@KotlinPoetMetadataPreview
fun buildSelfReferencesToMapFuncBuilder(entity: EntityDefinition, rootKey: TypeName, entityClass: ClassName) =
    FunSpec.builder("to${entity.name}Map")
        .receiver(Iterable::class.parameterizedBy(ResultRow::class))
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                ClassName("org.jetbrains.exposed.sql", "Alias")
                    .parameterizedBy(ClassName(entityClass.packageName, "${entityClass}Table")).copy(nullable = true)
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
                ClassName("org.jetbrains.exposed.sql", "Alias")
                    .parameterizedBy(ClassName(entityClass.packageName, "${entityClass}Table"))
                    .copy(nullable = true)
            ).defaultValue("null").build()
        )
        .returns(entityClass)

fun selfReferenceAssociationsMapping(it: AssociationDefinition, entity: EntityDefinition) =
    "\t${it.name} = this[${entity.name}Table.${it.name}Id]?.let { nextAlias?.let { this.to${it.target.simpleName}(nextAlias, null) } }"

@KotlinPoetMetadataPreview
fun buildSelfReferencesToEntityListFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
    val func = FunSpec.builder("to${entity.name}List")
        .receiver(Iterable::class.parameterizedBy(ResultRow::class))
        .returns(List::class.asClassName().parameterizedBy(entityType.toKmClass().toClassName()))
        .addParameter(
            ParameterSpec.builder(
                "nextAlias",
                ClassName("org.jetbrains.exposed.sql", "Alias")
                    .parameterizedBy(
                        ClassName(packageName = entityType.packageName, "${entityType.toKmClass().toClassName()}Table"))
                    .copy(nullable = true)
            ).defaultValue("null").build()
        )

    func.addStatement("return this.to${entity.name}Map(nextAlias).values.toList()")

    return func.build()
}
