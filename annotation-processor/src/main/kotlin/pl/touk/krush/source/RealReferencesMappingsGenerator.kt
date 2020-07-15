package pl.touk.krush.source

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.validation.EntityNotMappedException
import javax.lang.model.element.TypeElement

class RealReferencesMappingsGenerator : MappingsGenerator() {

    override fun buildToEntityMapFuncBody(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs, func: FunSpec.Builder,
                                          entityId: IdDefinition, rootKey: TypeName, rootVal: String, rootIdName: String, rootValId: String): FunSpec {
        func.addStatement("var roots = mutableMapOf<$rootKey, ${entity.name}>()")

        // Add all non-relational data
        func.addStatement("this.forEach { resultRow ->")
        func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entityId.name}) ?: return@forEach")
        func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
        func.addStatement("\troots[$rootValId] = $rootVal")
        func.addStatement("}")

        val associations = entity.getAssociations(ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val entityIdTypeName = entityId.asTypeName()
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
            val associationMapValueType = if (assoc.type in listOf(ONE_TO_MANY, MANY_TO_MANY)) "MutableSet<${target.name}>" else "${target.name}"

            func.addStatement("val $associationMapName = mutableMapOf<${entityIdTypeName}, $associationMapValueType>()")
            if (!(assoc.type == ONE_TO_ONE && assoc.mapped)) {
                val isSelfReferential = assoc.target == entityType

                // Prevent infinite recursions
                // (when the table is self-referential, roots will be used as "foreign map" - see below)
                if (!isSelfReferential) {
                    func.addStatement("val ${assoc.name}_map = this.to${assoc.target.simpleName}Map()")
                }
            }
        }

        // Add O2O relational data first, because it recreates the entity objects which destroys potential object references
        if (associations.any { it.type == ONE_TO_ONE }) {
            func.addStatement("this.forEach { resultRow ->")
            func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entityId.name}) ?: return@forEach")
            func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
            associations.forEach { assoc ->
                if (assoc.type != ONE_TO_ONE) {
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

                        func.addStatement("\t\t${assoc.name} = $value ?: $rootVal.${assoc.name}$sep")

                    }
            func.addStatement("\t)")
            func.addStatement("}.toMutableMap()")
        }

        // Add O2M and M2M relations
        if (associations.any { it.type == ONE_TO_MANY || it.type == MANY_TO_MANY }) {

            // Add list relational data (this is done in a separate step so that self-referential relations work)
            func.addStatement("this.forEach { resultRow ->")
            func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entityId.name}) ?: return@forEach")
            func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
            associations.forEach { assoc ->
                val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
                val targetVal = target.name.asVariable()
                val collName = "${assoc.name}_$rootVal"
                val associationMapName = "${entity.name.asVariable()}_${assoc.name}"

                // Use self as "foreign" table if the table is self-referential
                val isSelfReferential = assoc.target == entityType

                val foreignTableMap: String
                if (!isSelfReferential) {
                    foreignTableMap = "${assoc.name}_map"
                } else {
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
                if (assoc.type !in listOf(ONE_TO_MANY, MANY_TO_MANY)) {
                    return@forEach
                }

                val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
                func.addStatement("\t\tval ${associationMapName}_relations = $associationMapName[$rootVal.$rootIdName]?.toList()")
                func.addStatement("\t\tif (${associationMapName}_relations != null) {")
                func.addStatement("\t\t\t($rootVal.${assoc.name} as MutableList).addAll(${associationMapName}_relations)")
                func.addStatement("\t\t}")
            }
            func.addStatement("\t}")
        }

        func.addStatement("return roots")

        return func.build()
    }

}
