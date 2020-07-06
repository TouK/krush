package pl.touk.krush.source

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.exposed.sql.ResultRow
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.validation.EntityNotMappedException
import pl.touk.krush.validation.MissingIdException
import javax.lang.model.element.TypeElement

class CopiedReferencesMappingsGenerator : MappingsGenerator() {

    override fun buildToEntityMapFunc(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs): FunSpec {
        val rootKey = entity.id?.asTypeName() ?: throw MissingIdException(entity)

        val rootVal = entity.name.asVariable()
        val func = FunSpec.builder("to${entity.name}Map")
                .receiver(Iterable::class.parameterizedBy(ResultRow::class))
                .returns(ClassName("kotlin.collections", "MutableMap").parameterizedBy(rootKey, entityType.asType().asTypeName()))

        val rootIdName = entity.id.name.asVariable()
        val rootValId = "${rootVal}Id"

        func.addStatement("val roots = mutableMapOf<$rootKey, ${entity.name}>()")
        val associations = entity.getAssociations(ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val entityTypeName = entity.id.asTypeName()
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
            val associationMapValueType = if (assoc.type in listOf(ONE_TO_MANY, MANY_TO_MANY)) "MutableSet<${target.name}>" else "${target.name}"

            func.addStatement("val $associationMapName = mutableMapOf<${entityTypeName}, $associationMapValueType>()")
            if (!(assoc.type == ONE_TO_ONE && assoc.mapped)) {
                func.addStatement("val ${assoc.name}_map = this.to${assoc.target.simpleName}Map()")
            }
        }

        func.addStatement("this.forEach { resultRow ->")
        func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entity.id.name}) ?: return@forEach")
        func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
        func.addStatement("\troots[$rootValId] = $rootVal")
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val targetVal = target.name.asVariable()
            val collName = "${assoc.name}_$rootVal"
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"

            when (assoc.type) {
                ONE_TO_MANY, MANY_TO_MANY -> {
                    func.addStatement("\tresultRow.getOrNull(${target.idColumn})?.let {")
                    func.addStatement("\t\tval $collName = ${assoc.name}_map.filter { $targetVal -> $targetVal.key == it }")

                    val isBidirectional = target.associations.find { it.target == entityType }?.mapped ?: false
                    if (isBidirectional) {
                        func.addStatement("\t\t\t.mapValues { (_, $targetVal) -> $targetVal.copy($rootVal = $rootVal) }")
                    }

                    func.addStatement("\t\t\t.values.toMutableSet()")
                    func.addStatement("\t\t$associationMapName[$rootValId]?.addAll($collName) ?: $associationMapName.put($rootValId, $collName)")
                    func.addStatement("\t}")
                }

                ONE_TO_ONE -> {
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

                else -> {}
            }
        }
        func.addStatement("}")
        func.addStatement("return roots.mapValues { (_, $rootVal) ->")
        func.addStatement("\t${rootVal}.copy(")
        associations.forEachIndexed { idx, assoc ->
            val sep = if (idx == associations.lastIndex) "" else ","
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
            val value = if (assoc.type in listOf(ONE_TO_MANY, MANY_TO_MANY)) {
                "$associationMapName[$rootVal.$rootIdName]?.toList() ?: emptyList()"
            } else {
                "$associationMapName[$rootVal.$rootIdName]"
            }

            func.addStatement("\t\t${assoc.name} = $value$sep")

        }
        func.addStatement("\t)")
        func.addStatement("}.toMutableMap()")

        return func.build()
    }

}