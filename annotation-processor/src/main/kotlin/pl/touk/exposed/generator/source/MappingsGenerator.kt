package pl.touk.exposed.generator.source

import org.yanex.takenoko.kotlinFile
import org.yanex.takenoko.param
import org.yanex.takenoko.receiverType
import org.yanex.takenoko.returnType
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.asArgument
import pl.touk.exposed.generator.model.traverse

class MappingsGenerator : SourceGenerator {
    override fun generate(graph: EntityGraph, packageName: String): GeneratedFile {
        val generatedMappingFile = kotlinFile(packageName) {
            import("org.jetbrains.exposed.sql.ResultRow")
            import("org.jetbrains.exposed.sql.statements.UpdateBuilder")

            graph.traverse { entity ->
                import(entity.qualifiedName.toString())
            }

            graph.traverse { entity ->
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

                function("from") {
                    receiverType("UpdateBuilder<Any>")
                    val param = entity.name.asArgument()
                    param(param, entity.name.toString())
                    body {
                        val propsMappings = entity.getPropertyNames().map { name ->
                            "this[${entity.name}Table.${name}] = ${param}.${name}"
                        }
                        propsMappings.forEachIndexed { idx, mapping ->
                            val sep = if (idx + 1 < propsMappings.size) "\n" else ""
                            append("\t${mapping}${sep}")
                        }
                    }
                }
            }
        }

        return GeneratedFile(generatedMappingFile, "mappings.kt")
    }
}
