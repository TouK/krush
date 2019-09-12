package pl.touk.exposed.generator.source

import org.yanex.takenoko.kotlinFile
import org.yanex.takenoko.param
import org.yanex.takenoko.receiverType
import org.yanex.takenoko.returnType
import pl.touk.exposed.generator.validation.EntityNotMappedException
import pl.touk.exposed.generator.model.AssociationType
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.asVariable
import pl.touk.exposed.generator.model.traverse

class MappingsGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, packageName: String): GeneratedFile {
        val generatedMappingFile = kotlinFile(packageName) {
            import("org.jetbrains.exposed.sql.ResultRow")
            import("org.jetbrains.exposed.sql.statements.UpdateBuilder")

            graph.traverse { entity ->
                import(entity.qualifiedName.toString())
            }

            graph.traverse { entityType, entity ->
                // TODO split
                function("to${entity.name}") {
                    receiverType("ResultRow")
                    returnType(entity.name.toString())
                    body {
                        appendln("return ${entity.name}(")
                        val propsMappings = entity.getPropertyAndIdNames().map { name ->
                            "$name = this[${entity.name}Table.${name}]"
                        }
                        propsMappings.forEachIndexed { idx, mapping ->
                            val sep = if (idx + 1 < propsMappings.size) "," else ""
                            appendln("\t${mapping}${sep}")
                        }
                        append(")")
                    }
                }

                function("to${entity.name}s") {
                    receiverType("Iterable<ResultRow>")
                    returnType("List<${entity.name}>")
                    body {
                        val rootVal = entity.name.asVariable()
                        val rootValId = "${rootVal}Id"
                        // TODO: real id type
                        appendln("val roots = mutableMapOf<Long, ${entity.name}>()")
                        val associations = entity.getAssociations(AssociationType.ONE_TO_MANY, AssociationType.MANY_TO_MANY)
                        associations.forEach { assoc ->
                            val target = graph[assoc.target] ?: throw EntityNotMappedException(assoc.target)
                            appendln("val ${assoc.name} = mutableMapOf<Long, MutableList<${target.name}>>()" )
                        }
                        appendln("this.forEach { resultRow ->")
                        appendln("\tval $rootValId = resultRow[${entity.name}Table.id]")
                        appendln("\tval $rootVal = resultRow.to${entity.name}()")
                        appendln("\troots[$rootValId] = $rootVal")
                        associations.forEach { assoc ->
                            val target = graph[assoc.target] ?: throw EntityNotMappedException(assoc.target)
                            val targetVal = target.name.asVariable()
                            val collName = "${assoc.name}_$rootVal"
                            appendln("\tresultRow.getOrNull(${target.idColumn})?.let {")
                            // val phonesOfCustomer = phones.getOrDefault(customerId, mutableListOf())
                            appendln("\t\tval $collName = ${assoc.name}.getOrDefault($rootValId, mutableListOf())")
                            val isBidirectional = target.associations.find { it.target == entityType }?.mapped ?: false
                            // val phone = resultRow.toPhone().copy(customer = customer)
                            append("\t\tval $targetVal = resultRow.to${target.name}()")
                            if (isBidirectional) {
                                append(".copy($rootVal = $rootVal)")
                            }
                            append("\n")
                            appendln("\t\t$collName.add($targetVal)")
                            // phones[customerId] = phonesOfCustomer
                            appendln("\t\t${assoc.name}[$rootValId] = $collName")
                            appendln("\t}")
                        }
                        appendln("}")
                        appendln("return roots.mapValues { (_, $rootVal) ->")
                        appendln("\t${rootVal}.copy(")
                        associations.forEachIndexed { idx, assoc ->
                            val sep = if (idx == associations.lastIndex) "" else ","
                            appendln("\t\t${assoc.name} = ${assoc.name}[$rootVal.id]?.toList() ?: emptyList()$sep")
                        }
                        appendln("\t)")
                        appendln("}.values.toList()")
                    }
                }

                function("from") {
                    receiverType("UpdateBuilder<Any>")
                    val param = entity.name.asVariable()
                    param(param, entity.name.toString())
                    entity.associations.filter { !it.mapped }.forEach { assoc ->
                        param(assoc.target.simpleName.asVariable(), "${assoc.target.simpleName}?", "null")
                    }
                    val tableName = "${entity.name}Table"
                    body {
                        entity.getPropertyNames().forEach { name ->
                            append("\tthis[$tableName.$name] = $param.$name\n")
                        }
                        entity.associations.forEachIndexed { idx, assoc ->
                            val sep = if (idx == entity.associations.lastIndex) "\n" else ""
                            if (assoc.type == AssociationType.MANY_TO_ONE) {
                                val name = assoc.name
                                val targetParam = assoc.target.simpleName.asVariable()
                                if (assoc.mapped) {
                                    append("\tthis[$tableName.$name] = $param.$name?.id$sep")
                                } else {
                                    append("\t${targetParam}?.let { this[$tableName.$name] = it.id }$sep")
                                }
                            }
                        }
                    }
                }
            }
        }

        return GeneratedFile(generatedMappingFile, "mappings.kt")
    }
}
