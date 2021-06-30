package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import org.jetbrains.exposed.sql.ResultRow
import pl.touk.krush.meta.toClassName
import pl.touk.krush.model.*
import javax.lang.model.element.TypeElement

fun hasSelfReferences(entityType: TypeElement, entity: EntityDefinition): Boolean {
    return entity.getAssociations(
        AssociationType.ONE_TO_MANY,
        AssociationType.ONE_TO_ONE,
        AssociationType.MANY_TO_ONE,
        AssociationType.MANY_TO_MANY
    )
        .any { it.target == entityType }
}

@KotlinPoetMetadataPreview
fun buildToEntityFuncSelf(entityType: TypeElement, entity: EntityDefinition): FunSpec {
    val entityClass = entityType.toImmutableKmClass().toClassName()
    val func = FunSpec.builder("to${entity.name}")
        .receiver(ResultRow::class.java)
        .addParameter(
            "alias",
            ClassName("org.jetbrains.exposed.sql", "Alias")
                .parameterizedBy(ClassName("${entityType.packageName}", "${entityClass}Table"))
        )
        .addParameter(
            "parentAlias",
            ClassName("org.jetbrains.exposed.sql", "Alias")
                .parameterizedBy(ClassName("${entityType.packageName}", "${entityClass}Table"))
                .copy(nullable = true)
        )
        .returns(entityClass)

    val propsMappings = entity.getPropertyNames().map { name ->
        "\t$name = this[alias[${entity.name}Table.${name}]]"
    }

    val embeddedMappings = entity.embeddables.map { embeddable ->
        val embeddableName = embeddable.propertyName.asVariable()
        val embeddableMapping = embeddable.getPropertyNames().joinToString(", \n") { name ->
            val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
            "\t\t$name = this[alias${entity.name}Table.${tablePropName}Id]"
        }

        "\t$embeddableName = ${embeddable.qualifiedName}(\n$embeddableMapping\n\t)"
    }

    val associationsMappings = entity.getAssociations(AssociationType.MANY_TO_ONE, AssociationType.ONE_TO_ONE)
        .filter { assoc -> assoc.mapped }
        .map {
            if (!it.nullable) {
                "\t${it.name} = this.to${it.target.simpleName}()"
            } else {
                if (hasSelfReferences(entityType, entity)) {
                    //TODO: IM NOT SURE IF THIS IS CORRECT
                    "\t${it.name} = this[alias[${entity.name}Table.${it.name}Id]]?.let { parentAlias?.let{ this.to${it.target.simpleName}(parentAlias, alias)} }"
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
        (propsMappings + embeddedMappings + associationsMappings + listAssociationMapping).joinToString(",\n")

    func.addStatement("return %T(\n$mapping\n)", entityClass)

    return func.build()
}


@KotlinPoetMetadataPreview
fun buildSelfReferencesFuncBuilder(
    entity: EntityDefinition,
    rootKey: TypeName,
    entityType: TypeElement,
    entityClass: ClassName
) = FunSpec.builder("to${entity.name}Map")
    .receiver(Iterable::class.parameterizedBy(ResultRow::class))
    .addParameter(
        "parentAlias",
        ClassName("org.jetbrains.exposed.sql", "Alias")
            .parameterizedBy(
                ClassName(
                    "${entityType.packageName}",
                    "${entityClass}Table"
                )
            ).copy(nullable = true)
    )
    .returns(
        ClassName("kotlin.collections", "MutableMap").parameterizedBy(
            rootKey,
            entityType.toImmutableKmClass().toClassName()
        )
    )


fun buildSelfReferencesToEntityBuilder(
    entity: EntityDefinition,
    entityType: TypeElement,
    entityClass: ClassName
) = FunSpec.builder("to${entity.name}")
    .receiver(ResultRow::class.java)
    .addParameter(
        "parentAlias",
        ClassName(
            "org.jetbrains.exposed.sql",
            "Alias"
        ).parameterizedBy(ClassName("${entityType.packageName}", "${entityClass}Table"))
            .copy(nullable = true)
    )
    .returns(entityClass)


fun selfReferenceAssociationsMapping(it: AssociationDefinition, entity: EntityDefinition) =
    "\t${it.name} = this[${entity.name}Table.${it.name}Id]?.let { parentAlias?.let{ this.to${it.target.simpleName}(parentAlias, null)} }"

@KotlinPoetMetadataPreview
fun buildSelfReferencesToEntityListFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
    val func = FunSpec.builder("to${entity.name}List")
        .receiver(Iterable::class.parameterizedBy(ResultRow::class))
        .returns(List::class.asClassName().parameterizedBy(entityType.toImmutableKmClass().toClassName()))
        .addParameter(
            "parentAlias",
            ClassName("org.jetbrains.exposed.sql", "Alias")
                .parameterizedBy(
                    ClassName(
                       packageName =  "${entityType.packageName}",
                       "${entityType.toImmutableKmClass().toClassName()}Table"
                    )
                ).copy(nullable = true)
        )

    func.addStatement("return this.to${entity.name}Map(parentAlias).values.toList()")

    return func.build()
}
