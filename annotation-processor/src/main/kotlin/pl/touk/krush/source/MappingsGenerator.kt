package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.meta.toClassName
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.validation.EntityNotMappedException
import pl.touk.krush.validation.MissingIdException
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
abstract class MappingsGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String, typeEnv: TypeEnvironment): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "mappings")
                .addImport("org.jetbrains.exposed.sql", "ResultRow")
                .addImport("org.jetbrains.exposed.sql.statements", "UpdateBuilder")

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, "${entity.simpleName}", "${entity.simpleName}Table",
                        "to${entity.simpleName}", "to${entity.simpleName}Map", "to${entity.simpleName}List")
            }
        }

        graph.traverse { entityType, entity ->
            fileSpec.addImport(entityType.packageName, entity.name.toString())
        }

        graph.traverse { entityType, entity ->
            fileSpec.addFunction(buildToEntityFunc(entityType, entity))

            if (hasSelfReferences(entityType, entity)) {
                fileSpec.addFunction(buildToEntityFuncSelf(entityType, entity))
            }

            if (hasSelfReferences(entityType, entity)) {
                fileSpec.addFunction(buildSelfReferencesToEntityListFunc(entityType, entity))
            }else {
                fileSpec.addFunction(buildToEntityListFunc(entityType, entity))
            }

            fileSpec.addFunction(buildToEntityMapFunc(entityType, entity, graphs))
            buildFromEntityFunc(entityType, entity)?.let { funSpec ->
                fileSpec.addFunction(funSpec)
            }
            entity.getAssociations(MANY_TO_MANY).forEach { assoc ->
                fileSpec.addFunction(buildFromManyToManyFunc(entityType, entity, assoc))
            }
        }

        return fileSpec.build()
    }

    private fun buildToEntityFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val entityClass = entityType.toImmutableKmClass().toClassName()
        val func = if (hasSelfReferences(entityType, entity)) {
             buildSelfReferencesToEntityBuilder(entity, entityType, entityClass)
        } else {
            buildNormalToEntityFunctionBuilder(entity, entityClass)
        }

        val idMapping = entity.id?.let { id ->
            if (id.embedded) {
                val embeddableIdName = id.name.asVariable()
                val embeddableIdMapping = id.properties.joinToString(", \n") { property ->
                    val name = property.name
                    "\t\t$name = this[${entity.tableName}.${id.propName(property)}]"
                }
                "\t$embeddableIdName = ${id.qualifiedName}(\n$embeddableIdMapping\n\t)"
            } else {
                "\t${id.name} = this[${entity.tableName}.${id.name}]"
            }
        }?.let { listOf(it) } ?: emptyList()

        val propsMappings = entity.getPropertyNames().map { name ->
            "\t$name = this[${entity.tableName}.${name}]"
        }

        val embeddedMappings = entity.embeddables.map { embeddable ->
            val embeddableName = embeddable.propertyName.asVariable()

            if (embeddable.nullable) {
                val embeddableMapping = embeddable.properties.joinToString(", \n") { property ->
                    val name = property.name
                    val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                    val denull = if (!property.nullable) "!!" else ""
                    "\t\t$name = this[${entity.tableName}.${tablePropName}]$denull"
                }
                val condition = embeddable.properties.filterNot(PropertyDefinition::nullable).map { property ->
                    val tablePropName = embeddable.propertyName.asVariable() + property.name.asVariable().capitalize()
                    "\t\tthis[${entity.tableName}.${tablePropName}] != null"
                }.joinToString(" &&\n")
                "\t$embeddableName = if (\n$condition\n\t) ${embeddable.qualifiedName}(\n$embeddableMapping\n\t) else null"
            } else {
                val embeddableMapping = embeddable.getPropertyNames().joinToString(", \n") { name ->
                    val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                    "\t\t$name = this[${entity.tableName}.${tablePropName}]"
                }
                "\t$embeddableName = ${embeddable.qualifiedName}(\n$embeddableMapping\n\t)"
            }
        }

        val associationsMappings = entity.getAssociations(MANY_TO_ONE, ONE_TO_ONE)
            .filter { assoc -> assoc.mapped }
            .map { assoc ->
                if (!assoc.nullable) {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}()"
                } else {
                    if (hasSelfReferences(entityType, entity)) {
                        selfReferenceAssociationsMapping(assoc, entity)
                    } else {
                        "\t${assoc.name} = this[${entity.name}Table.${assoc.defaultIdPropName()}]?.let { this.to${assoc.target.simpleName}() }"
                    }
                }
            }

        // Add empty but mutable lists for O2M and M2M connections, so that the relations can be filled in later
        // without possibly breaking existing references to this object
        val listAssociationMapping = entity.getAssociations(ONE_TO_MANY, MANY_TO_MANY)
                .map { "\t${it.name} = mutableListOf()" }

        val mapping = (idMapping + propsMappings + embeddedMappings + associationsMappings + listAssociationMapping).joinToString(",\n")

        func.addStatement("return %T(\n$mapping\n)", entityClass)

        return func.build()
    }

    private fun buildNormalToEntityFunctionBuilder(
        entity: EntityDefinition,
        entityClass: ClassName
    ) = FunSpec.builder("to${entity.name}")
        .receiver(ResultRow::class.java)
        .returns(entityClass)

    private fun buildToEntityListFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val func = FunSpec.builder("to${entity.name}List")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(List::class.asClassName().parameterizedBy(entityType.toImmutableKmClass().toClassName()))

        func.addStatement("return this.to${entity.name}Map().values.toList()")

        return func.build()
    }


    private fun buildToEntityMapFunc(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs): FunSpec {
        val rootKey = entity.id?.asUnderlyingTypeName() ?: throw MissingIdException(entity)
        val entityClass = entityType.toImmutableKmClass().toClassName()
        val rootVal = entity.name.asVariable()

        val func = if (hasSelfReferences(entityType, entity)) {
            buildSelfReferencesFuncBuilder(entity, rootKey, entityType, entityClass)
        } else {
            buildNormalFuncBuilder(entity, rootKey, entityType)
        }
        val rootIdName = entity.id.name.asVariable()
        val rootValId = "${rootVal}Id"

        return buildToEntityMapFuncBody(entityType, entity, graphs, func, entity.id, rootKey, rootVal, rootIdName, rootValId)
    }

    private fun buildNormalFuncBuilder(
        entity: EntityDefinition,
        rootKey: TypeName,
        entityType: TypeElement
    ) = FunSpec.builder("to${entity.name}Map")
        .receiver(Iterable::class.parameterizedBy(ResultRow::class))
        .returns(
            ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                rootKey,
                entityType.toImmutableKmClass().toClassName()
            )
        )

    protected fun addIdStatement(entity: EntityDefinition, id: IdDefinition, idVal: String, func: FunSpec.Builder) {
        if (id.embedded) {
            id.properties.forEach { property ->
                val propName = id.propName(property)
                func.addStatement("\tval $propName = resultRow.getOrNull(${entity.tableName}.$propName)")
            }
            val condition = id.properties.filterNot(PropertyDefinition::nullable).map { property ->
                "\t${id.propName(property)} != null"
            }.joinToString(" &&\n").takeIf { it.isNotBlank() } ?: "false"
            func.addStatement("\tval $idVal = if (\n$condition\n\t) ${id.qualifiedName}(${id.propsAsArgs}) else null")
        } else {
            func.addStatement("\tval $idVal = resultRow.getOrNull(${entity.tableName}.${id.name})")
        }
    }

    abstract fun buildToEntityMapFuncBody(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs, func: FunSpec.Builder,
                                          entityId: IdDefinition, rootKey: TypeName, rootVal: String, rootIdName: String, rootValId: String): FunSpec

    private fun buildFromEntityFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec? {
        val param = entity.name.asVariable() + "Source"
        val tableName = entity.tableName

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.asClassName().parameterizedBy(STAR))
                .addParameter(param, entityType.toImmutableKmClass().toClassName())

        entityAssocParams(entity).forEach { func.addParameter(it) }

        val idMapping = entity.id?.let { id ->
            if (id.embedded) {
                val embeddableIdName = id.name.asVariable()
                id.properties.map { property ->
                    val name = property.name
                    "\tthis[$tableName.${id.propName(property)}] = $param.$embeddableIdName.$name"
                }
            } else if (!id.generatedValue) {
                listOf("\tthis[$tableName.${entity.id.name}] = $param.${entity.id.name}")
            } else emptyList()
        } ?: emptyList()

        val propsMappings = entity.getPropertyNames().map { name ->
            "\tthis[$tableName.$name] = $param.$name"
        }

        val embeddedMappings = entity.embeddables.flatMap { embeddable ->
            val embeddableName = embeddable.propertyName.asVariable()
            val nullCheck = if (embeddable.nullable) "?" else ""
            embeddable.getPropertyNames().map { name ->
                val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                "\tthis[$tableName.$tablePropName] = $param.$embeddableName$nullCheck.$name"
            }
        }

        val assocToProcess =
            (entity.getAssociations(MANY_TO_ONE) + entity.getAssociations(ONE_TO_ONE).filter { it.mapped })
            .filter { it.sharedId == null }

        val assocMappings = assocToProcess.flatMap { assoc ->
            val name = assoc.name
            val targetIdVal = assoc.targetId.name.asVariable()
            if (assoc.mapped) {
                val sep = if (assoc.nullable) "?" else ""
                assoc.targetId.properties.map { targetIdProp ->
                    val targetIdPropVal = targetIdProp.name.asVariable()
                    val targetIdPropName = assoc.targetIdPropName(targetIdProp)
                    val path = when {
                        assoc.targetId.embedded -> "$param.$name$sep.$targetIdVal$sep.$targetIdPropVal"
                        else -> "$param.$name$sep.$targetIdVal"
                    }
                    "\tthis[$tableName.$targetIdPropName] = $path"
                }
            } else {
                val targetParam = assoc.target.simpleName.asVariable()
                val defaultIdPropName = assoc.defaultIdPropName()
                listOf("\t${targetParam}?.let { this[$tableName.$defaultIdPropName] = it.${assoc.targetId.name} }")
            }
        }

        val statements = (idMapping + propsMappings + embeddedMappings + assocMappings)
        if (statements.isEmpty()) return null

        statements.forEach { func.addStatement(it) }

        return func.build()
    }

    private fun buildFromManyToManyFunc(entityType: TypeElement, entity: EntityDefinition, assoc: AssociationDefinition): FunSpec {
        val (sourceSuffix, targetSuffix) = if (assoc.isSelfReferential) "Source" to "Target" else "" to ""
        val sourceParam = entity.name.asVariable() + sourceSuffix
        val targetVal = assoc.target.simpleName.asVariable()
        val targetParam = targetVal + targetSuffix
        val targetType = assoc.target
        val tableName = "${entity.name}${assoc.name.asObject()}Table"
        val entityId = entity.id ?: throw EntityNotMappedException(entityType)

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.asClassName().parameterizedBy(STAR))
                .addParameter(sourceParam, entityType.toImmutableKmClass().toClassName())
                .addParameter(targetParam, targetType.toImmutableKmClass().toClassName())

        listOf(Triple(entityType, entityId, sourceSuffix), Triple(targetType, assoc.targetId, targetSuffix)).forEach { (type, id, side) ->
            val rootVal = type.simpleName.asVariable()
            val rootPath = if (id.embedded) "$rootVal$side.${id.name.asVariable()}" else rootVal + side
            when (id.nullable) {
                true -> {
                    id.properties.forEach { idProp ->
                        val propName = "$rootVal$side${idProp.valName.capitalize()}"
                        func.addStatement("\t$rootPath.${idProp.valName}?.let { v -> this[$tableName.$propName] = v }")
                    }
                }
                false -> {
                    id.properties.forEach { idProp ->
                        val propName = "$rootVal$side${idProp.valName.capitalize()}"
                        func.addStatement("\tthis[$tableName.$propName] = $rootPath.${idProp.valName}")
                    }
                }
            }
        }

        return func.build()

    }

    private fun entityAssocParams(entity: EntityDefinition): List<ParameterSpec> {
        return entity.associations.filter { !it.mapped }.map { assoc ->
            ParameterSpec.builder(
                    assoc.target.simpleName.asVariable(),
                    assoc.target.toImmutableKmClass().toClassName().copy(nullable = true)
            ).defaultValue("null").build()
        }
    }

}
