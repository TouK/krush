package pl.touk.exposed.generator.source

import org.jetbrains.exposed.sql.Table
import org.yanex.takenoko.KoFile
import org.yanex.takenoko.KoType
import org.yanex.takenoko.kotlinFile
import org.yanex.takenoko.stringLiteral
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.TypeDefinition
import pl.touk.exposed.generator.model.traverse

class TablesGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, packageName: String): GeneratedFile {
        val generatedTableFile = kotlinFile(packageName) {
            import("org.jetbrains.exposed.sql.Table")

            graph.traverse { entity ->
                objectDeclaration("${entity.name}Table") {
                    extends(KoType.parseType(Table::class.java), stringLiteral(entity.table))
                    entity.id?.let { id ->
                        val name = id.name.toString()
                        property(name) {
                            var initializer = "long(\"$name\").primaryKey()"
                            if (id.generatedValue) {
                                initializer += ".autoIncrement()"
                            }
                            initializer(initializer)
                        }
                    }
                    entity.properties.forEach { column ->
                        val name = column.name.toString()
                        property(name) {
                            val initializer = when (column.type) {
                                TypeDefinition.STRING -> "varchar(\"$name\", ${column.annotation.length})"
                                TypeDefinition.LONG -> "long(\"$name\")"
                                TypeDefinition.BOOL -> "bool(\"$name\")"
                                TypeDefinition.DATE -> "date(\"name\")"
                                TypeDefinition.DATETIME -> "datetime(\"name\")"
                            }
                            initializer(initializer)
                        }
                    }
                }
            }
        }

        return GeneratedFile(generatedTableFile, "tables.kt")
    }
}
