package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.meta.toClassName
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
                        "to${entity.simpleName}", "to${entity.simpleName}Map", "to${entity.simpleName}List", "addSubEntitiesTo${entity.simpleName}")
            }
        }

        graph.traverse { entityType, entity ->
            fileSpec.addImport(entityType.packageName, entity.name.toString())
        }

        graph.traverse { entityType, entity ->
            // Functions for reading objects from the DB
            fileSpec.addFunction(buildToEntityFunc(entityType, entity))
            fileSpec.addFunction(buildToEntityListFunc(entityType, entity))
            fileSpec.addFunction(buildAddSubEntitiesToEntityFunc(entityType, entity))
            fileSpec.addFunction(buildToEntityMapFunc(entityType, entity, graphs))

            // Functions for inserting objects into the DB
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

        val entityCacheType = ClassName("kotlin.collections", "MutableMap")
            .parameterizedBy(
                String::class.asClassName(),
                ClassName("kotlin.collections", "MutableMap").parameterizedBy(ANY, ANY)
            )

        val func = FunSpec.builder("to${entity.name}")
                .receiver(ResultRow::class.java)
                .addParameter(
                    ParameterSpec.builder("entityCache", entityCacheType)
                        .defaultValue("mutableMapOf()")
                        .build()
                )
                .returns(entityClass)

        val idReadingCode = idReadingBlock(entity.id!!, entity.tableName)

        func.addStatement("val ${entity.id.name.asVariable()} = $idReadingCode")

        func.apply {
            addStatement("val cacheMap = entityCache[\"${entityType.simpleName}\"] ?: mutableMapOf<Any, Any>()")
            addStatement("if(cacheMap[${entity.id.name.asVariable()}] != null) {")
            addStatement("\treturn cacheMap[${entity.id.name.asVariable()}] as ${entityType.simpleName}")
            addStatement("}")
        }

        val idMapping = listOf("\t${entity.id.name} = ${entity.id.name.asVariable()}")

        val propertyMappings = entity.getPropertyNames().map { name ->
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

        val manyToOneAssociationsMappings = entity.getAssociations(MANY_TO_ONE)
            .filter { assoc -> assoc.mapped }
            .map { assoc ->
                if(!assoc.mapped) {
                    "\t${assoc.name} = this.getOrNull(${assoc.targetTable}.${assoc.targetId.name})?.let { this.to${assoc.target.simpleName}(entityCache) }"
                } else if (!assoc.nullable) {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}(entityCache)"
                } else {
                    "\t${assoc.name} = this[${entity.tableName}.${assoc.defaultIdPropName()}]?.let { this.to${assoc.target.simpleName}(entityCache) }"
                }
            }

        val oneToOneAssociations = entity.getAssociations(ONE_TO_ONE)
            .map { assoc ->
                if (!assoc.mapped) {
                    if(assoc.nullable) {
                        // This will be replaced by a copy()-call just before this object is returned.
                        "\t${assoc.name} = null"
                    } else {
                        "\t${assoc.name} = this.to${assoc.target.simpleName}(entityCache)"
                    }
                } else if (!assoc.nullable) {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}(entityCache)"
                } else {
                    "\t${assoc.name} = this.getOrNull(${assoc.targetTable}.${assoc.targetId.name})?.let { this.to${assoc.target.simpleName}(entityCache) }"
                }
            }

        val mappedOneToOneAssociations = entity.getAssociations(ONE_TO_ONE)
            .filter { assoc -> !assoc.mapped }
            .joinToString(",\n") { assoc ->
                if(assoc.nullable) {
                    "\t${assoc.name} = this.getOrNull(${assoc.targetTable}.${assoc.targetId.name})?.let { this.to${assoc.target.simpleName}(entityCache) }"
                } else {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}(entityCache)"
                }
            }

        // Add empty but mutable lists for O2M and M2M connections, so that the relations can be filled in later
        // without possibly breaking existing references to this object
        val listAssociationMapping = entity.getAssociations(ONE_TO_MANY, MANY_TO_MANY)
                .map { "\t${it.name} = mutableListOf()" }

        val mapping = (idMapping + propertyMappings + embeddedMappings + manyToOneAssociationsMappings + oneToOneAssociations + listAssociationMapping)
            .joinToString(",\n")

        func.apply {
            addStatement("val result = %T(\n$mapping\n)", entityClass)
            addStatement("cacheMap[${entity.id.name.asVariable()}] = result")
            addStatement("entityCache[\"${entityType.simpleName}\"] = cacheMap")

            if(mappedOneToOneAssociations.isBlank()) {
                addStatement("return result")
            } else {
                addComment("Add bijective O2O references after caching the object to avoid infinite loops")
                addStatement("return result.copy(\n$mappedOneToOneAssociations\n)")
            }
        }

        return func.build()
    }

    private fun idReadingBlock(id: IdDefinition, tableName: String, rowReference: String = "this"): String {
        return if (id.embedded) {
            val embeddableIdMapping = id.properties.joinToString(", \n") { property ->
                val name = property.name
                "\t\t$name = $rowReference[${tableName}.${id.propName(property)}]"
            }
            "${id.qualifiedName}(\n$embeddableIdMapping\n\t)"
        } else {
            "$rowReference[${tableName}.${id.name}]"
        }
    }

    private fun buildToEntityListFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val func = FunSpec.builder("to${entity.name}List")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(List::class.asClassName().parameterizedBy(entityType.toImmutableKmClass().toClassName()))

        func.addStatement("return this.to${entity.name}Map().values.toList()")

        return func.build()
    }

    private fun buildAddSubEntitiesToEntityFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val entityParamName = entity.name.asVariable()

        val entityCacheType = ClassName("kotlin.collections", "MutableMap")
            .parameterizedBy(
                String::class.asClassName(),
                ClassName("kotlin.collections", "MutableMap").parameterizedBy(ANY, ANY)
            )

        val func = FunSpec.builder("addSubEntitiesTo${entity.name}")
            .receiver(ResultRow::class)
            .addParameter(entityParamName, entityType.toImmutableKmClass().toClassName().copy(nullable = true))
            .addParameter(
                ParameterSpec.builder("entityCache", entityCacheType)
                    .defaultValue("mutableMapOf()")
                    .build()
            )

        func.addStatement("if($entityParamName == null) return")

        // Recursively add info to every related O2O entity
        entity.getAssociations(ONE_TO_ONE).forEach { oneToOneAssoc ->
            func.addComment("Add sub-elements contained in this row to ${oneToOneAssoc.name}")
            func.addStatement("addSubEntitiesTo${oneToOneAssoc.target.simpleName}($entityParamName.${oneToOneAssoc.name}, entityCache)")
        }

        // M2M and M2O relations are represented as lists. When such a list contains multiple entities, those entities
        // are spread over multiple rows. Here, we figure out whether this row contains a new sub-entity in a list and
        // add it to that list if necessary. Otherwise, we just recursively search each entity for sub-sub-entities that
        // might be new in this row, etc.
        entity.getAssociations(ONE_TO_MANY, MANY_TO_MANY).forEach { setAssoc ->

            val attrValName = "${setAssoc.name.asVariable()}Attr"
            val newEntityValName = "new${setAssoc.target.simpleName}"

            val targetTypeName = "${setAssoc.target.simpleName}"

            func.apply {
                addStatement("val ${setAssoc.name.asVariable()}Id = ${idReadingBlock(setAssoc.targetId, setAssoc.targetTable)}")
                addStatement("if(${setAssoc.name.asVariable()}Id != null) {")


                addStatement("\tval $attrValName = $entityParamName.${setAssoc.name.asVariable()} as MutableList<$targetTypeName>")
                addStatement("\tval ${attrValName}LastElement = $attrValName.lastOrNull()")

                addStatement("\tif(${setAssoc.name.asVariable()}Id != ${attrValName}LastElement?.${setAssoc.targetId.name}) {")

                addComment("\t\tIf the sub-entity is new, create a new object for it")
                addStatement("\t\tval $newEntityValName = to$targetTypeName()")
                addStatement("\t\taddSubEntitiesTo$targetTypeName($newEntityValName, entityCache)")
                addStatement("\t\t$attrValName.add($newEntityValName)")

                addStatement("\t} else {")

                addComment("\t\tIf we already have an entity with this ID, check if there's a new sub-sub-entity in it")
                addStatement("\t\taddSubEntitiesTo$targetTypeName(${attrValName}LastElement, entityCache)")

                addStatement("\t}")

                addStatement("}")
            }
        }

        return func.build()
    }

    private fun buildToEntityMapFunc(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs): FunSpec {
        val rootKey = entity.id?.asUnderlyingTypeName() ?: throw MissingIdException(entity)

        val func = FunSpec.builder("to${entity.name}Map")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(ClassName("kotlin.collections", "MutableMap").parameterizedBy(rootKey, entityType.toImmutableKmClass().toClassName()))


        val entityMapValName = "${entity.name.asVariable()}Map"
        val entityIdValName = "${entity.name.asVariable()}Id"
        val currentEntityValName = "current${entity.name.asObject()}"

        func.apply {
            addStatement("val $entityMapValName = mutableMapOf<${entity.id.asUnderlyingTypeName()}, ${entity.name}>()")
            addStatement("val entityCache: MutableMap<String, MutableMap<Any, Any>> = mutableMapOf()")

            addStatement("this.forEach { row ->")

            addStatement("\tval $entityIdValName = ${idReadingBlock(entity.id, entity.tableName, rowReference = "row")}")

            addComment("Create this entity or expand on the sub-entity lists contained within")
            addStatement("\tval $currentEntityValName = $entityMapValName[$entityIdValName] ?: row.to${entity.name}(entityCache)")
            addStatement("\trow.addSubEntitiesTo${entity.name}($currentEntityValName, entityCache)")
            addStatement("\t$entityMapValName[$entityIdValName] = $currentEntityValName")

            addStatement("}")

            addStatement("return $entityMapValName")
        }

        return func.build()
    }

    protected fun addIdStatement(entity: EntityDefinition, id: IdDefinition, idVal: String, func: FunSpec.Builder)  {
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
        val param = entity.name.asVariable()
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
