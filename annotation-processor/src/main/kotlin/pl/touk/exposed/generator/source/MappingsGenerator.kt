package pl.touk.exposed.generator.source

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import pl.touk.exposed.generator.model.AssociationDefinition
import pl.touk.exposed.generator.model.AssociationType
import pl.touk.exposed.generator.model.EntityDefinition
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.EntityGraphs
import pl.touk.exposed.generator.model.allAssociations
import pl.touk.exposed.generator.model.asObject
import pl.touk.exposed.generator.model.asVariable
import pl.touk.exposed.generator.model.packageName
import pl.touk.exposed.generator.model.traverse
import pl.touk.exposed.generator.validation.EntityNotMappedException
import javax.lang.model.element.TypeElement

class MappingsGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "mappings")
                .addImport("org.jetbrains.exposed.sql", "ResultRow")
                .addImport("org.jetbrains.exposed.sql.statements", "UpdateBuilder")

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, "${entity.simpleName}", "${entity.simpleName}Table", "to${entity.simpleName}")
            }
        }

        graph.traverse { entityType, entity ->
            fileSpec.addImport(entityType.packageName, entity.name.toString())
        }

        graph.traverse { entityType, entity ->
            fileSpec.addFunction(buildToEntityFunc(entityType, entity))
            fileSpec.addFunction(buildToEntityListFunc(entityType, entity, graphs))
            fileSpec.addFunction(buildFromEntityFunc(entityType, entity))
            entity.getAssociations(AssociationType.MANY_TO_MANY).forEach { assoc ->
                fileSpec.addFunction(buildFromManyToManyFunc(entityType, entity, assoc))
            }
        }

        return fileSpec.build()
    }

    private fun buildToEntityFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val func = FunSpec.builder("to${entity.name}")
                .receiver(ResultRow::class.java)
                .returns(entityType.asType().asTypeName())

        val propsMappings = entity.getPropertyAndIdNames().joinToString(",\n") { name ->
            "\t$name = this[${entity.name}Table.${name}]"
        }

        func.addStatement("return %T(\n$propsMappings\n)", entityType.asType().asTypeName())

        return func.build()
    }

    private fun buildToEntityListFunc(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs): FunSpec {
        val func = FunSpec.builder("to${entity.name}List")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(List::class.asClassName().parameterizedBy(entityType.asType().asTypeName()))

        val rootVal = entity.name.asVariable()
        val rootValId = "${rootVal}Id"

        // TODO: real id type
        func.addStatement("val roots = mutableMapOf<Long, ${entity.name}>()")
        val associations = entity.getAssociations(AssociationType.ONE_TO_MANY, AssociationType.MANY_TO_MANY)
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            func.addStatement("val ${assoc.name} = mutableMapOf<Long, MutableList<${target.name}>>()" )
        }
        func.addStatement("this.forEach { resultRow ->")
        func.addStatement("\tval $rootValId = resultRow[${entity.name}Table.id]")
        func.addStatement("\tval $rootVal = resultRow.to${entity.name}()")
        func.addStatement("\troots[$rootValId] = $rootVal")
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val targetVal = target.name.asVariable()
            val collName = "${assoc.name}_$rootVal"
            func.addStatement("\t\tresultRow.getOrNull(${target.idColumn})?.let {")
            // val phonesOfCustomer = phones.getOrDefault(customerId, mutableListOf())
            func.addStatement("\t\tval $collName = ${assoc.name}.getOrDefault($rootValId, mutableListOf())")
            val isBidirectional = target.associations.find { it.target == entityType }?.mapped ?: false
            // val phone = resultRow.toPhone().copy(customer = customer)
            func.addStatement("\t\tval $targetVal = resultRow.to${target.name}()")
            if (isBidirectional) {
                func.addStatement("\t\t\t.copy($rootVal = $rootVal)")
            }
            func.addStatement("\t\t$collName.add($targetVal)")
            // phones[customerId] = phonesOfCustomer
            func.addStatement("\t\t${assoc.name}[$rootValId] = $collName")
            func.addStatement("\t}")
        }
        func.addStatement("}")
        func.addStatement("return roots.mapValues { (_, $rootVal) ->")
        func.addStatement("\t${rootVal}.copy(")
        associations.forEachIndexed { idx, assoc ->
            val sep = if (idx == associations.lastIndex) "" else ","
            func.addStatement("\t\t${assoc.name} = ${assoc.name}[$rootVal.id]?.toList() ?: emptyList()$sep")
        }
        func.addStatement("\t)")
        func.addStatement("}.values.toList()")

        return func.build()
    }

    private fun buildFromEntityFunc(entityType: TypeElement, entity: EntityDefinition): FunSpec {
        val param = entity.name.asVariable()
        val tableName = "${entity.name}Table"

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.parameterizedBy(Any::class))
                .addParameter(param, entityType.asType().asTypeName())

        val assocParams = entity.associations.filter { !it.mapped }.map { assoc ->
            ParameterSpec.builder(
                    assoc.target.simpleName.asVariable(),
                    assoc.target.asType().asTypeName().copy(nullable = true)
            ).defaultValue("null").build()
        }

        assocParams.forEach { func.addParameter(it) }

        val propsMappings = entity.getPropertyNames().map { name ->
            "\tthis[$tableName.$name] = $param.$name"
        }

        val assocMappings = entity.getAssociations(AssociationType.MANY_TO_ONE).map { assoc ->
            val name = assoc.name
            val targetParam = assoc.target.simpleName.asVariable()
            if (assoc.mapped) {
                "\tthis[$tableName.$name] = $param.$name?.id"
            } else {
                "\t${targetParam}?.let { this[$tableName.$name] = it.id }"
            }
        }

        (propsMappings + assocMappings).forEach {
            func.addStatement(it)
        }

        return func.build()
    }

    private fun buildFromManyToManyFunc(entityType: TypeElement, entity: EntityDefinition, assoc: AssociationDefinition): FunSpec {
        val param = entity.name.asVariable()
        val targetVal = assoc.target.simpleName.asVariable()
        val targetType = assoc.target
        val tableName = "${entity.name}${assoc.name.asObject()}Table"

        val func = FunSpec.builder("from")
                .receiver(UpdateBuilder::class.parameterizedBy(Any::class))
                .addParameter(param, entityType.asType().asTypeName())
                .addParameter(targetVal, targetType.asClassName())

        listOf(param, targetVal).forEach { side ->
            func.addStatement("\t${side}.id?.let { id -> this[$tableName.${side}Id] = id }")
        }

        return func.build()

    }

}
