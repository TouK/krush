package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.validation.EntityNotMappedException
import pl.touk.krush.validation.MissingIdException
import javax.lang.model.element.TypeElement

class MappingsGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String, typeEnv: TypeEnvironment): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "mappings")
                .addImport("org.jetbrains.exposed.sql", "ResultRow")
                .addImport("org.jetbrains.exposed.sql.statements", "UpdateBuilder")

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, "${entity.simpleName}", "${entity.simpleName}Table",
                        "to${entity.simpleName}", "to${entity.simpleName}Map")
            }
        }

        graph.traverse { entityType, entity ->
            fileSpec.addImport(entityType.packageName, entity.name.toString())
        }

        graph.traverse { entityType, entity ->
            fileSpec.addFunction(buildToEntityFunc(entityType, entity))
            fileSpec.addFunction(buildToEntityListFunc(entityType, entity))
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
        val func = FunSpec.builder("to${entity.name}")
                .receiver(ResultRow::class.java)
                .returns(entityType.asType().asTypeName())

        val propsMappings = entity.getPropertyAndIdNames().map { name ->
            "\t$name = this[${entity.name}Table.${name}]"
        }

        val embeddedMappings = entity.embeddables.map { embeddable ->
            val embeddableName = embeddable.propertyName.asVariable()
            val embeddableMapping = embeddable.getPropertyNames().joinToString(", \n") { name ->
                val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                "\t\t$name = this[${entity.name}Table.${tablePropName}]"
            }

            "\t$embeddableName = ${embeddable.qualifiedName}(\n$embeddableMapping\n\t)"
        }

        val associationsMappings = entity.getAssociations(MANY_TO_ONE, ONE_TO_ONE)
                .filter { assoc -> assoc.mapped }
                .map { "\t${it.name} = this.to${it.target.simpleName}()"}

        // Add empty but mutable lists for O2M and M2M connections, so that the relations can be filled in later
        // without possibly breaking existing references to this object
        val listAssociationMapping = entity.getAssociations(ONE_TO_MANY, MANY_TO_MANY)
                .map { "\t${it.name} = mutableListOf()" }

        val mapping = (propsMappings + embeddedMappings + associationsMappings + listAssociationMapping).joinToString(",\n")

        func.addStatement("return %T(\n$mapping\n)", entityType.asType().asTypeName())

        return func.build()
    }

    private fun buildToEntityListFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val func = FunSpec.builder("to${entity.name}List")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(List::class.asClassName().parameterizedBy(entityType.asType().asTypeName()))

        func.addStatement("return this.to${entity.name}Map().values.toList()")

        return func.build()
    }

    private fun buildToEntityMapFunc(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs): FunSpec {
        val rootKey = entity.id?.asTypeName() ?: throw MissingIdException(entity)

        val rootVal = entity.name.asVariable()
        val func = FunSpec.builder("to${entity.name}Map")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(ClassName("kotlin.collections", "MutableMap").parameterizedBy(rootKey, entityType.asType().asTypeName()))

        val rootIdName = entity.id.name.asVariable()
        val rootValId = "${rootVal}Id"

        func.addStatement("var roots = mutableMapOf<$rootKey, ${entity.name}>()")

        // Add all non-relational data
        func.addStatement("this.forEach { resultRow ->")
        func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entity.id.name}) ?: return@forEach")
        func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
        func.addStatement("\troots[$rootValId] = $rootVal")
        func.addStatement("}")

        val associations = entity.getAssociations(ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val entityTypeName = entity.id.asTypeName()
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
            val associationMapValueType = if (assoc.type in listOf(ONE_TO_MANY, MANY_TO_MANY)) "MutableSet<${target.name}>" else "${target.name}"

            func.addStatement("val $associationMapName = mutableMapOf<${entityTypeName}, $associationMapValueType>()")
            if (!(assoc.type == ONE_TO_ONE && assoc.mapped)) {
                val isSelfReferential = assoc.target == entityType

                // Prevent infinite recursions
                // (when the table is self-referential, roots will be used as "foreign map" - see below)
                if(!isSelfReferential){
                    func.addStatement("val ${assoc.name}_map = this.to${assoc.target.simpleName}Map()")
                }

            }
        }

        // Add O2O relational data first, because it recreates the entity objects which destroys potential object references
        if(associations.any { it.type == ONE_TO_ONE }){
            func.addStatement("this.forEach { resultRow ->")
            func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entity.id.name}) ?: return@forEach")
            func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
            associations.forEach { assoc ->
                if(assoc.type != ONE_TO_ONE){
                    return@forEach
                }

                val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
                val associationMapName = "${entity.name.asVariable()}_${assoc.name}"

                if (!assoc.mapped) {
                    func.addStatement("\tresultRow.getOrNull(${target.idColumn})?.let {")
                    func.addStatement("\t\t${assoc.name}_map.get(it)?.let {")
                    func.addStatement("\t\t\t$associationMapName[$rootValId] = it")
                    func.addStatement("\t\t}")
                    func.addStatement("\t}")
                } else {
                    func.addStatement("\tval ${assoc.name.asVariable()} = resultRow.to${target.name}()")
                    func.addStatement("\t$associationMapName[${rootValId}] =  ${assoc.name.asVariable()}")
                }
            }
            func.addStatement("}")

            func.addStatement("roots = roots.mapValues { (_, $rootVal) ->")
            func.addStatement("\t${rootVal}.copy(")
            associations
                    .filter { it.type == ONE_TO_ONE }
                    .forEachIndexed { idx, assoc ->
                        val sep = if (idx == associations.lastIndex) "" else ","
                        val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
                        val value = "$associationMapName[$rootVal.$rootIdName]"

                        func.addStatement("\t\t${assoc.name} = $value!!$sep")

                    }
            func.addStatement("\t)")
            func.addStatement("}.toMutableMap()")
        }

        // Add O2M and M2M relations
        if(associations.any { it.type == ONE_TO_MANY || it.type == MANY_TO_MANY }){

            // Add list relational data (this is done in a separate step so that self-referential relations work)
            func.addStatement("this.forEach { resultRow ->")
            func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entity.id.name}) ?: return@forEach")
            func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
            associations.forEach { assoc ->
                val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
                val targetVal = target.name.asVariable()
                val collName = "${assoc.name}_$rootVal"
                val associationMapName = "${entity.name.asVariable()}_${assoc.name}"

                // Use self as "foreign" table if the table is self-referential
                val isSelfReferential = assoc.target == entityType

                val foreignTableMap: String
                if(!isSelfReferential){
                    foreignTableMap = "${assoc.name}_map"
                }else{
                    foreignTableMap = "roots"
                }

                when (assoc.type) {
                    ONE_TO_MANY -> {
                        func.addStatement("\tresultRow.getOrNull(${target.idColumn})?.let {")
                        func.addStatement("\t\tval $collName = $foreignTableMap.filter { $targetVal -> $targetVal.key == it }")

                        val isBidirectional = target.associations.find { it.target == entityType }?.mapped ?: false
                        if (isBidirectional) {
                            func.addStatement("\t\t\t.mapValues { (_, $targetVal) -> $targetVal.copy($rootVal = $rootVal) }")
                        }

                        func.addStatement("\t\t\t.values.toMutableSet()")
                        func.addStatement("\t\t$associationMapName[$rootValId]?.addAll($collName) ?: $associationMapName.put($rootValId, $collName)")
                        func.addStatement("\t}")
                    }

                    MANY_TO_MANY -> {
                        val assocTableTargetIdCol = "${entity.name}${assoc.name.asObject()}Table.${targetVal}TargetId"

                        func.addStatement("\tresultRow.getOrNull($assocTableTargetIdCol)?.let {")
                        func.addStatement("\t\tval $collName = $foreignTableMap.filter { $targetVal -> $targetVal.key == it }")

                        func.addStatement("\t\t\t.values.toMutableSet()")
                        func.addStatement("\t\t$associationMapName[$rootValId]?.addAll($collName) ?: $associationMapName.put($rootValId, $collName)")
                        func.addStatement("\t}")
                    }

                    else -> {}
                }
            }
            func.addStatement("}")

            func.addStatement("roots.forEach { (_, $rootVal) ->")
            associations.forEach { assoc ->
                if(assoc.type !in listOf(ONE_TO_MANY, MANY_TO_MANY)){
                    return@forEach
                }

                val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
                func.addStatement("\t\tval ${associationMapName}_relations = $associationMapName[$rootVal.$rootIdName]?.toList()")
                func.addStatement("\t\tif(${associationMapName}_relations != null){")
                func.addStatement("\t\t\t($rootVal.${assoc.name} as MutableList).addAll(${associationMapName}_relations)")
                func.addStatement("\t\t}")
            }
            func.addStatement("\t}")
        }

        func.addStatement("return roots")

        return func.build()
    }

    private fun buildFromEntityFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec? {
        val param = entity.name.asVariable()
        val tableName = "${entity.name}Table"

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.parameterizedBy(Any::class))
                .addParameter(param, entityType.asType().asTypeName())

        entityAssocParams(entity).forEach { func.addParameter(it) }

        val idMapping = when (entity.id?.generatedValue) {
            false -> listOf("\tthis[$tableName.${entity.id.name}] = $param.${entity.id.name}")
            else -> emptyList()
        }

        val propsMappings = entity.getPropertyNames().map { name ->
            "\tthis[$tableName.$name] = $param.$name"
        }

        val embeddedMappings = entity.embeddables.map { embeddable ->
            val embeddableName = embeddable.propertyName.asVariable()
            embeddable.getPropertyNames().map { name ->
                val tablePropName = embeddable.propertyName.asVariable() + name.asVariable().capitalize()
                "\tthis[$tableName.$tablePropName] = $param.$embeddableName.$name"
            }
        }.flatten()

        val assocMappings = entity.getAssociations(MANY_TO_ONE).map { assoc ->
            val name = assoc.name
            val targetParam = assoc.target.simpleName.asVariable()
            if (assoc.mapped) {
                "\tthis[$tableName.$name] = $param.$name?.${assoc.targetId.name.asVariable()}"
            } else {
                "\t${targetParam}?.let { this[$tableName.$name] = it.${assoc.targetId.name} }"
            }
        }

        val oneToOneMappings = entity.getAssociations(ONE_TO_ONE).filter { it.mapped }.map { assoc ->
            val name = assoc.name
            "\tthis[$tableName.$name] = $param.$name?.${assoc.targetId.name.asVariable()}"
        }

        val statements = (idMapping + propsMappings + embeddedMappings + assocMappings + oneToOneMappings)
        if (statements.isEmpty()) return null

        statements.forEach { func.addStatement(it) }

        return func.build()
    }

    private fun buildFromManyToManyFunc(entityType: TypeElement, entity: EntityDefinition, assoc: AssociationDefinition): FunSpec {
        val param = entity.name.asVariable() + "Source"
        val targetVal = assoc.target.simpleName.asVariable()
        val param2 = targetVal + "Target"
        val targetType = assoc.target
        val tableName = "${entity.name}${assoc.name.asObject()}Table"
        val entityId = entity.id ?: throw EntityNotMappedException(entityType)

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.parameterizedBy(Any::class))
                .addParameter(param, entityType.asType().asTypeName())
                .addParameter(param2, targetType.asClassName())

        listOf(Pair(param, entityId), Pair(param2, assoc.targetId)).forEach { side ->
            when (side.second.nullable) {
                true -> func.addStatement("\t${side.first}.id?.let { id -> this[$tableName.${side.first}Id] = id }")
                false -> func.addStatement("\tthis[$tableName.${side.first}Id] = ${side.first}.id")
            }
        }

        return func.build()

    }

    private fun entityAssocParams(entity: EntityDefinition): List<ParameterSpec> {
        return entity.associations.filter { !it.mapped }.map { assoc ->
            ParameterSpec.builder(
                    assoc.target.simpleName.asVariable(),
                    assoc.target.asType().asTypeName().copy(nullable = true)
            ).defaultValue("null").build()
        }
    }

}
