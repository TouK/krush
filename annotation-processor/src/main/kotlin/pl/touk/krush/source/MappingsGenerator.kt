package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toKmClass
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import pl.touk.krush.RowWrapper
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.meta.toClassName
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.validation.EntityNotMappedException
import pl.touk.krush.validation.MissingIdException
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
class MappingsGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String, typeEnv: TypeEnvironment): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "mappings")
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .addMember("%S", "UNUSED_PARAMETER")
                    .build()
            )
            .addImport("org.jetbrains.exposed.sql", "ResultRow")
            .addImport("org.jetbrains.exposed.sql.statements", "UpdateBuilder")
            .addImport("kotlin.reflect", "KClass")

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, entity.simpleName, "${entity.simpleName}Table",
                        "to${entity.simpleName}", "to${entity.simpleName}Map", "to${entity.simpleName}List", "addSubEntitiesTo${entity.simpleName}")
            }
        }

        graph.traverse { entityType, entity ->
            fileSpec.addImport(entityType.packageName, entity.name.toString())
        }

        graph.traverse { entityType, entity ->
            // Functions for reading objects from the DB
            val hasSelfRef = entity.hasSelfReferentialAssoc()
            val entityClass = entityType.toClassName()
            fileSpec.addFunction(buildToEntityFunc(hasSelfRef, entityClass, entity))
            if (hasSelfRef) {
                fileSpec.addFunction(buildToEntityFuncSelf(entityType, entity))
            }
            fileSpec.addFunction(buildRowToEntityFunc(hasSelfRef, entityClass, entity))
            fileSpec.addFunction(buildToEntityListFunc(entityClass, entity))
            if (hasSelfRef) {
                fileSpec.addFunction(buildSelfReferencesToEntityListFunc(entityType, entity))
            }
            fileSpec.addFunction(buildAddSubEntitiesToEntityFunc(entityClass, entity))
            fileSpec.addFunction(buildToEntityMapFunc(hasSelfRef, entityClass, entity, graphs))

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

    private fun buildToEntityFunc(hasSelfRef: Boolean, entityClass: ClassName, entity: EntityDefinition): FunSpec {
        val func = if (hasSelfRef) {
            buildSelfReferencesToEntityBuilder(entity, entityClass)
        } else {
            FunSpec.builder("to${entity.name}")
                .receiver(RowWrapper::class.java)
                .returns(entityClass)
        }

        val idReadingCode = idReadingBlock(entity.id!!, entity.tableName, rowReference = "row")

        func.addStatement("val ${entity.id.name.asVariable()} = $idReadingCode")

        func.addStatement("val cacheMap = entityStore.getOrPut(${entityClass.simpleName}::class) { mutableMapOf() }")

        val idMapping = listOf("\t${entity.id.name} = ${entity.id.name.asVariable()}")

        val propertyMappings = entity.getPropertyNames().map { name ->
            "\t$name = row[${entity.tableName}.${name}]"
        }

        val embeddedMappings = entity.embeddables.map { embeddable ->
            val embeddableName = embeddable.propertyName.asVariable()

            if (embeddable.nullable) {
                val embeddableMapping = embeddable.properties.joinToString(", \n") { property ->
                    val name = property.name
                    val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                    val denull = if (!property.nullable) "!!" else ""
                    "\t\t$name = row[${entity.tableName}.${tablePropName}]$denull"
                }
                val condition = embeddable.properties.filterNot(PropertyDefinition::nullable).map { property ->
                    val tablePropName = embeddable.propertyName.asVariable() + property.name.asVariable().capitalize()
                    "\t\trow[${entity.tableName}.${tablePropName}] != null"
                }.joinToString(" &&\n")
                "\t$embeddableName = if (\n$condition\n\t) ${embeddable.qualifiedName}(\n$embeddableMapping\n\t) else null"
            } else {
                val embeddableMapping = embeddable.getPropertyNames().joinToString(", \n") { name ->
                    val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                    "\t\t$name = row[${entity.tableName}.${tablePropName}]"
                }
                "\t$embeddableName = ${embeddable.qualifiedName}(\n$embeddableMapping\n\t)"
            }
        }

        val manyToOneAssociationsMappings = entity.getAssociations(MANY_TO_ONE)
            .filter { assoc -> assoc.mapped }
            .map { assoc ->
                if (!assoc.mapped) {
                    "\t${assoc.name} = row.getOrNull(${assoc.targetTable}.${assoc.targetId.name})?.let { this.to${assoc.target.simpleName}() }"
                } else if (!assoc.nullable) {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}()"
                } else {
                    if (assoc.isSelfReferential) {
                        "\t${assoc.name} = row[${entity.tableName}.${assoc.defaultIdPropName()}]?.let { nextAlias?.let { this.to${assoc.target.simpleName}(nextAlias) } }"
                    } else {
                        "\t${assoc.name} = row[${entity.tableName}.${assoc.defaultIdPropName()}]?.let { this.to${assoc.target.simpleName}() }"
                    }
                }
            }

        val oneToOneAssociations = entity.getAssociations(ONE_TO_ONE)
            .map { assoc ->
                if (!assoc.mapped) {
                    if (assoc.nullable) {
                        // This will be replaced by a copy()-call just before this object is returned.
                        "\t${assoc.name} = null"
                    } else {
                        "\t${assoc.name} = this.to${assoc.target.simpleName}()"
                    }
                } else if (!assoc.nullable) {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}()"
                } else {
                    if (assoc.isSelfReferential) {
                        "\t${assoc.name} = row.getOrNull(${assoc.targetTable}.${assoc.name}Id)?.let { nextAlias?.let { this.to${assoc.target.simpleName}(nextAlias) } }"
                    } else {
                        "\t${assoc.name} = row.getOrNull(${assoc.targetTable}.${assoc.targetId.name})?.let { this.to${assoc.target.simpleName}() }"
                    }
                }
            }

        val mappedOneToOneAssociations = entity.getAssociations(ONE_TO_ONE)
            .filter { assoc -> !assoc.mapped }
            .joinToString(",\n") { assoc ->
                if (assoc.nullable) {
                    "\t${assoc.name} = row.getOrNull(${assoc.targetTable}.${assoc.targetId.name})?.let { this.to${assoc.target.simpleName}() }"
                } else {
                    "\t${assoc.name} = this.to${assoc.target.simpleName}()"
                }
            }

        // Add empty but mutable lists for O2M and M2M connections, so that the relations can be filled in later
        // without possibly breaking existing references to this object
        val listAssociationMapping = entity.getAssociations(ONE_TO_MANY, MANY_TO_MANY)
                .map { "\t${it.name} = mutableListOf()" }

        val mapping = (idMapping + propertyMappings + embeddedMappings + manyToOneAssociationsMappings + oneToOneAssociations + listAssociationMapping)
            .joinToString(",\n")

        func.apply {
            addStatement("return cacheMap.getOrPut(${entity.id.name.asVariable()}) {")
            
            if (mappedOneToOneAssociations.isBlank()) {
                addStatement("\t%T(\n$mapping\n)", entityClass)
            } else {
                addStatement("\tval partial${entity.name} = %T(\n$mapping\n)", entityClass)
                addStatement("\tcacheMap[${entity.id.name.asVariable()}] = partial${entity.name}")
                addComment("\tAdd bijective O2O references after caching the object to avoid infinite loops")
                addStatement("\treturn@getOrPut partial${entity.name}.copy(\n$mappedOneToOneAssociations\n)")
            }
            addStatement("} as %T", entityClass)
        }

        return func.build()
    }

    private fun idReadingBlock(id: IdDefinition, tableName: String, rowReference: String = "this", nullable: Boolean = false): String {
        return if (id.embedded) {
            val embeddableIdMapping = id.properties.joinToString(", \n") { property ->
                val name = property.name
                "\t\t$name = $rowReference[${tableName}.${id.propName(property)}]"
            }
            if (!nullable) {
                "${id.qualifiedName}(\n$embeddableIdMapping\n\t)"
            } else {
                val nullCheck = id.properties.joinToString(" && ") { property ->
                    "$rowReference.getOrNull(${tableName}.${id.propName(property)}) == null"
                }
                "if ($nullCheck) null else ${id.qualifiedName}(\n$embeddableIdMapping\n\t)"
            }

        } else {
            if (!nullable) {
                "$rowReference[${tableName}.${id.name}]"
            } else {
                "$rowReference.getOrNull(${tableName}.${id.name})"
            }
        }
    }

    private fun buildRowToEntityFunc(hasSelfRef: Boolean, entityClass: ClassName, entity: EntityDefinition): FunSpec {
        return if (hasSelfRef) {
            FunSpec.builder("to${entity.name}")
                .receiver(ResultRow::class.java)
                .addParameter(
                    ParameterSpec.builder(
                        "nextAlias",
                        ClassName("org.jetbrains.exposed.sql", "Alias")
                            .parameterizedBy(ClassName(entityClass.packageName, "${entityClass}Table"))
                            .copy(nullable = true)
                    ).defaultValue("null").build()
                )
                .returns(entityClass)
                .addStatement("return %T(this).to${entity.name}(nextAlias)", RowWrapper::class)
                .build()
        } else {
            FunSpec.builder("to${entity.name}")
                .receiver(ResultRow::class.java)
                .returns(entityClass)
                .addStatement("return %T(this).to${entity.name}()", RowWrapper::class)
                .build()
        }
    }

    private fun buildToEntityListFunc(entityClass: ClassName, entity: EntityDefinition): FunSpec {
        val func = FunSpec.builder("to${entity.name}List")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(List::class.asClassName().parameterizedBy(entityClass))

        func.addStatement("return this.to${entity.name}Map().values.toList()")

        return func.build()
    }

    private fun buildAddSubEntitiesToEntityFunc(entityClass: ClassName, entity: EntityDefinition): FunSpec {
        val entityParamName = entity.name.asVariable()

        val func = FunSpec.builder("addSubEntitiesTo${entity.name}")
            .receiver(RowWrapper::class)
            .addParameter(entityParamName, entityClass.copy(nullable = true))

        func.addStatement("if ($entityParamName == null) return")

        // Recursively add info to every related O2O entity
        entity.getAssociations(ONE_TO_ONE).forEach { oneToOneAssoc ->
            if (!oneToOneAssoc.isSelfReferential) {
                func.addComment("Add sub-elements contained in this row to ${oneToOneAssoc.name}")
                func.addStatement("addSubEntitiesTo${oneToOneAssoc.target.simpleName}($entityParamName.${oneToOneAssoc.name})")
            }
        }

        // M2M and M2O relations are represented as lists. When such a list contains multiple entities, those entities
        // are spread over multiple rows. Here, we figure out whether this row contains a new sub-entity in a list and
        // add it to that list if necessary. Otherwise, we just recursively search each entity for sub-sub-entities that
        // might be new in this row, etc.
        entity.getAssociations(ONE_TO_MANY, MANY_TO_MANY).forEach { setAssoc ->

            if (setAssoc.type == ONE_TO_MANY || !setAssoc.isSelfReferential) {
                val assocVar = setAssoc.name.asVariable()
                val assocVarId = "${assocVar}Id"
                val attrValName = "${assocVarId}Attr"
                val newEntityValName = "new${setAssoc.target.simpleName}"

                val targetTypeName = "${setAssoc.target.simpleName}"

                func.apply {
                    // Allowing a null id here allows users to not include a join with the other table if they don't
                    // need the relation-lists to be populated
                    addStatement("val $assocVarId = ${idReadingBlock(setAssoc.targetId, setAssoc.targetTable, nullable = true, rowReference = "row")}")
                    beginControlFlow("if ($assocVarId != null) {")

                    addStatement("val $attrValName = $entityParamName.$assocVar as MutableList<$targetTypeName>")
                    addStatement("val ${attrValName}LastElement = $attrValName.lastOrNull()")

                    if (setAssoc.isBidirectional && entity.id != null) {
                        addComment("Prevent stack overflow when mapping bi-directional relations")
                        val entityId = "$entityParamName.${entity.id.name}"
                        if (setAssoc.isSelfReferential) {
                            beginControlFlow("if ($assocVarId != $entityId) {")
                        }
                        beginControlFlow("withoutEntity(%T::class, $entityId) {", entityClass)
                    }

                    beginControlFlow("if (${setAssoc.name.asVariable()}Id != ${attrValName}LastElement?.${setAssoc.targetId.name}) {")

                    addComment("If the sub-entity is new, create a new object for it")
                    addStatement("val $newEntityValName = to$targetTypeName()")
                    addStatement("addSubEntitiesTo$targetTypeName($newEntityValName)")
                    addStatement("$attrValName.add($newEntityValName)")

                    addStatement("} else {")

                    addComment("\tIf we already have an entity with this ID, check if there's a new sub-sub-entity in it")
                    addStatement("\taddSubEntitiesTo$targetTypeName(${attrValName}LastElement)")

                    endControlFlow()

                    if (setAssoc.isBidirectional && entity.id != null) {
                        if (setAssoc.isSelfReferential) {
                            endControlFlow()
                        }
                        endControlFlow()
                    }

                    endControlFlow()
                }
            } else {
                val id = entity.id!!
                val relationTableName = "${entity.name}${setAssoc.name.asObject()}Table"
                val idReadingBlock = if (id.embedded) {
                    val embeddableIdMapping = id.properties.joinToString(", \n") { property ->
                        val name = property.name
                        val targetColumnName = "${entity.name.asVariable()}Target${property.valName.capitalize()}"
                        "\t\t$name = row[$relationTableName.$targetColumnName]"
                    }
                    val nullCheck = id.properties.joinToString(" && ") { property ->
                        val targetColumnName = "${entity.name.asVariable()}Target${property.valName.capitalize()}"
                        "row.getOrNull($relationTableName.$targetColumnName) == null"
                    }
                    "if ($nullCheck) null else ${id.qualifiedName}(\n$embeddableIdMapping\n\t)"

                } else {
                    val targetColumnName = "${entity.name.asVariable()}Target${entity.id.name.toString().capitalize()}"
                    "row.getOrNull($relationTableName.$targetColumnName)"
                }

                val selfReferenceMapName = "${entity.name.asVariable()}SelfReferenceRequests"

                func.apply {
                    // Allowing a null id here allows users to not include a join with the other table if they don't
                    // need the relation-lists to be populated
                    addStatement("val other${entity.name}Id = $idReadingBlock")
                    addStatement("if (other${entity.name}Id != null) {")
                    addStatement("\tval $selfReferenceMapName = selfReferenceRequests.getOrPut(%T::class) { mutableMapOf() }", entityClass)
                    addStatement("\tval ${entity.name.asVariable()}Requests = $selfReferenceMapName.getOrPut(other${entity.name}Id) { mutableSetOf() }")
                    addStatement("\t${entity.name.asVariable()}Requests.add($entityParamName.${id.name}!!)")
                    addStatement("}")
                }
            }
        }

        return func.build()
    }

    private fun buildToEntityMapFunc(
        hasSelfRef: Boolean, entityClass: ClassName, entity: EntityDefinition, graphs: EntityGraphs
    ): FunSpec {
        val rootKey = entity.id?.asUnderlyingTypeName() ?: throw MissingIdException(entity)

        val func = if (hasSelfRef) {
            buildSelfReferencesToMapFuncBuilder(entity, rootKey, entityClass)
        } else {
            FunSpec.builder("to${entity.name}Map")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(
                    ClassName("kotlin.collections", "Map").parameterizedBy(rootKey, entityClass)
                )
        }
        
        val currentEntityValName = "current${entity.name.asObject()}"

        func.apply {
            addStatement("val entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()")
            addStatement("val selfReferenceRequests: MutableMap<KClass<*>, MutableMap<Any, MutableSet<Any>>> = mutableMapOf()")

            addStatement("this.forEach { row ->")

            addComment("\tCreate this entity or expand on the sub-entity lists contained within")
            addStatement("\tval rowWrapper = %T(row, entityStore, selfReferenceRequests)", RowWrapper::class)
            if (hasSelfRef) {
                addStatement("\tval $currentEntityValName = rowWrapper.to${entity.name}(nextAlias)")
            } else {
                addStatement("\tval $currentEntityValName = rowWrapper.to${entity.name}()")
            }
            addStatement("\trowWrapper.addSubEntitiesTo${entity.name}($currentEntityValName)")

            addStatement("}")

            val selfRefAssociations = DFS(graphs).visit(entity.type)
                .flatMap { it.associations }
                .filter { it.isSelfReferential }

            val selfRefAssociationsFiltered = selfRefAssociations
                // filter out bidirectional associations processed twice
                .filterNot { selfRefAssoc -> selfRefAssociations.any { it != selfRefAssoc && it.target == selfRefAssoc.source && it.isBidirectional } }

            if (selfRefAssociationsFiltered.isNotEmpty()) {
                // Go through all self references requested and add them to the respective list.
                addStatement("selfReferenceRequests.forEach { (clazz, unsatisfiedMap) -> ")
                addStatement("\twhen(clazz) {")

                selfRefAssociationsFiltered
                    .forEach { selfRefAssoc ->
                        val entityName = selfRefAssoc.source.simpleName
                        val subjectIdName = "subject${entityName}Id"
                        val referencingIdSetName = "referencing${entityName}Ids"
                        val subjectValName = "subject${entityName}"
                        val referencingIdName = "referencing${entityName}Id"
                        val referencingEntityName = "referencing${selfRefAssoc.target.simpleName}"
                        val targetType = selfRefAssoc.target
                        val targetClass = targetType.toClassName()

                        addStatement("\t\t%T::class -> unsatisfiedMap.forEach { ($subjectIdName, $referencingIdSetName) ->", targetClass)
                        addStatement("\t\t\tval $subjectValName = entityStore[%T::class]?.get($subjectIdName) as? $entityName", targetClass)
                        addStatement("\t\t\tif ($subjectValName != null) {")
                        addStatement("\t\t\t\t$referencingIdSetName.forEach { $referencingIdName -> ")
                        addStatement("\t\t\t\t\tval $referencingEntityName = entityStore[%T::class]?.get($referencingIdName) as? %T", targetClass, targetClass)
                        addStatement("\t\t\t\t\t($referencingEntityName?.${selfRefAssoc.name.asVariable()} as? MutableList<$entityName>)?.add($subjectValName)")
                        addStatement("\t\t\t\t}")
                        addStatement("\t\t\t}")
                        addStatement("\t\t}")
                    }

                addStatement("\t}")
                addStatement("}")
            }

            addStatement("return (entityStore[%T::class] ?: emptyMap()) as Map<$rootKey, ${entity.name}>", entityClass)
        }

        return func.build()
    }

    private fun buildFromEntityFunc(entityType: Type, entity: EntityDefinition): FunSpec? {
        val param = entity.name.asVariable()
        val tableName = entity.tableName

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.asClassName().parameterizedBy(STAR))
                .addParameter(param, entityType.toClassName())

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

    private fun buildFromManyToManyFunc(entityType: Type, entity: EntityDefinition, assoc: AssociationDefinition): FunSpec {
        val (sourceSuffix, targetSuffix) = if (assoc.isSelfReferential) "Source" to "Target" else "" to ""
        val sourceParam = entity.name.asVariable() + sourceSuffix
        val targetVal = assoc.target.simpleName.asVariable()
        val targetParam = targetVal + targetSuffix
        val targetType = assoc.target
        val tableName = "${entity.name}${assoc.name.asObject()}Table"
        val entityId = entity.id ?: throw EntityNotMappedException(entityType)

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.asClassName().parameterizedBy(STAR))
                .addParameter(sourceParam, entityType.toClassName())
                .addParameter(targetParam, targetType.toClassName())

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
                    assoc.target.toClassName().copy(nullable = true)
            ).defaultValue("null").build()
        }
    }

}
