package pl.touk.krush.source

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import pl.touk.krush.model.*
import pl.touk.krush.model.AssociationType.*
import pl.touk.krush.validation.EntityNotMappedException
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
class CopiedReferencesMappingsGenerator : MappingsGenerator() {

    override fun buildToEntityMapFuncBody(entityType: TypeElement, entity: EntityDefinition, graphs: EntityGraphs, func: FunSpec.Builder,
                                          entityId: IdDefinition, rootKey: TypeName, rootVal: String, rootIdName: String, rootValId: String): FunSpec {
        func.addStatement("val roots = mutableMapOf<$rootKey, ${entity.name}>()")
        val associations = entity.getAssociations(ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val entityIdTypeName = entityId.asUnderlyingTypeName()
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"
            val associationMapValueType = if (assoc.type in listOf(ONE_TO_MANY, MANY_TO_MANY)) "MutableSet<${target.name}>" else "${target.name}"

            func.addStatement("val $associationMapName = mutableMapOf<${entityIdTypeName}, $associationMapValueType>()")
            if (!(assoc.type == ONE_TO_ONE && assoc.mapped)) {
                val isSelfReferential = assoc.target == entityType

                // Prevent infinite recursions
                // (when the table is self-referential, roots will be used as "foreign map" - see below)
                if (!isSelfReferential) {
                    func.addStatement("val ${assoc.name}_map = this.to${assoc.target.simpleName}Map()")
                } else {
                    // TODO should be logged at validation phase
                    func.addStatement("val ${assoc.name}_map = emptyMap<${entityIdTypeName}, $entityType>()")
                }
            }
        }

        func.addStatement("this.forEach { resultRow ->")
        func.addStatement("\tval $rootValId = resultRow.getOrNull(${entity.name}Table.${entityId.name}) ?: return@forEach")
        func.addStatement("\tval $rootVal = roots[$rootValId] ?: resultRow.to${entity.name}()")
        func.addStatement("\troots[$rootValId] = $rootVal")
        associations.forEach { assoc ->
            val target = graphs[assoc.target.packageName]?.get(assoc.target) ?: throw EntityNotMappedException(assoc.target)
            val targetVal = target.name.asVariable()
            val collName = "${assoc.name}_$rootVal"
            val associationMapName = "${entity.name.asVariable()}_${assoc.name}"

            when (assoc.type) {
                ONE_TO_MANY, MANY_TO_MANY -> {
                    val isSelfReferential = assoc.target == entityType
                    func.addStatement("\tresultRow.getOrNull(${target.idColumn})?.let {")
                    func.addStatement("\t\tval $collName = ${assoc.name}_map.filter { $targetVal -> $targetVal.key == it }")

                    val isBidirectional = target.associations.find { it.target == entityType }?.mapped ?: false
                    if (isBidirectional && !isSelfReferential) {
                        func.addStatement("\t\t\t.mapValues { (_, $targetVal) -> $targetVal.copy($rootVal = $rootVal) }")
                    }

                    func.addStatement("\t\t\t.values.toMutableSet()")
                    func.addStatement("\t\t$associationMapName[$rootValId]?.addAll($collName) ?: $associationMapName.put($rootValId, $collName)")
                    func.addStatement("\t}")
                }

                ONE_TO_ONE -> {
                    val assocVar = assoc.name.asVariable()
                    if (!assoc.mapped) {
                        func.addStatement("\tresultRow.getOrNull(${target.idColumn})?.let {")
                        func.addStatement("\t\t${assoc.name}_map.get(it)?.let {")
                        func.addStatement("\t\t\t$associationMapName[$rootValId] = it")
                        func.addStatement("\t\t}")
                        func.addStatement("\t}")
                    } else if (assoc.nullable) {
                        func.addStatement("\tval $assocVar = resultRow[${entity.name}Table.${assoc.name}]?.let { resultRow.to${target.name}() }")
                        func.addStatement("\t$assocVar?.let { $associationMapName[${rootValId}] = it }")
                    } else {
                        func.addStatement("\tval $assocVar = resultRow.to${target.name}()")
                        func.addStatement("\t$associationMapName[${rootValId}] = $assocVar")
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
