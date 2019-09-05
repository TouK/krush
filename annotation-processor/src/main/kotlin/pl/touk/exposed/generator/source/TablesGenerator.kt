package pl.touk.exposed.generator.source

import org.jetbrains.exposed.sql.Table
import org.yanex.takenoko.KoFile
import org.yanex.takenoko.KoType
import org.yanex.takenoko.kotlinFile
import org.yanex.takenoko.stringLiteral
import pl.touk.exposed.generator.model.AssociationType
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.PropertyType
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
                            // TODO real id type
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
                                PropertyType.STRING -> "varchar(\"$name\", ${column.annotation.length})"
                                PropertyType.LONG -> "long(\"$name\")"
                                PropertyType.BOOL -> "bool(\"$name\")"
                                PropertyType.DATE -> "date(\"name\")"
                                PropertyType.DATETIME -> "datetime(\"name\")"
                            }
                            initializer(initializer)
                        }
                    }
                    entity.associations.forEach { association ->
                        val name = association.name.toString()
                        val columnName = association.joinColumn ?: "${name}_id"
                        val targetTable = "${association.target.simpleName}Table"
                        if (association.type == AssociationType.MANY_TO_ONE) {
                            property(name) {
                                initializer("long(\"$columnName\").references($targetTable.id).nullable()")
                            }
                        }
                    }
                }
            }
        }

        return GeneratedFile(generatedTableFile, "tables.kt")
    }
}
