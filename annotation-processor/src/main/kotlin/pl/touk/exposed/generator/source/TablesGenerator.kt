package pl.touk.exposed.generator.source

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import pl.touk.exposed.generator.model.AssociationType
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.EntityGraphs
import pl.touk.exposed.generator.model.PropertyType
import pl.touk.exposed.generator.model.allAssociations
import pl.touk.exposed.generator.model.packageName
import pl.touk.exposed.generator.model.traverse

class TablesGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "tables")
                .addImport("org.jetbrains.exposed.sql", "Table")

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, "${entity.simpleName}Table")
            }
        }

        graph.traverse { entity ->
            val objectSpec = TypeSpec.objectBuilder("${entity.name}Table")
                    .superclass(Table::class)
                    .addSuperclassConstructorParameter(CodeBlock.of("%S", entity.table))

            entity.id?.let { id ->
                val name = id.name.toString()
                // TODO id type
                val idSpec = PropertySpec.builder(name, Column::class.parameterizedBy(Long::class))
                var initializer = "long(\"$name\").primaryKey()"
                if (id.generatedValue) {
                    initializer += ".autoIncrement()"
                }
                idSpec.initializer(initializer)
                objectSpec.addProperty(idSpec.build())
            }
            entity.properties.forEach { column ->
                val name = column.name.toString()
                val columnType = Column::class.asTypeName().parameterizedBy(column.type.asTypeName() ?: column.typeMirror.asTypeName())
                val propSpec = PropertySpec.builder(name, columnType)
                val initializer = when (column.type) {
                    PropertyType.STRING -> CodeBlock.of("varchar(%S, %L)", name, column.annotation.length)
                    PropertyType.LONG -> CodeBlock.of("long(%S)", name)
                    PropertyType.BOOL -> CodeBlock.of("bool(%S)", name)
                    PropertyType.DATE -> CodeBlock.of("date(%S)", name)
                    PropertyType.DATETIME -> CodeBlock.of("datetime(%S)", name)
                }
                propSpec.initializer(initializer)
                objectSpec.addProperty(propSpec.build())
            }
            entity.associations.forEach { association ->
                val name = association.name.toString()
                val columnName = association.joinColumn ?: "${name}_id"
                val targetTable = "${association.target.simpleName}Table"
                if (association.type == AssociationType.MANY_TO_ONE) {
                    val columnType = Column::class.asClassName().parameterizedBy(Long::class.asClassName().copy(nullable = true))
                    objectSpec.addProperty(
                            PropertySpec.builder(name, columnType)
                                    .initializer("long(\"$columnName\").references($targetTable.id).nullable()")
                                    .build()
                    )
                }
            }
            fileSpec.addType(objectSpec.build())
        }

        return fileSpec.build()
    }
}

private fun PropertyType.asTypeName(): TypeName? {
    return when (this) {
        PropertyType.STRING -> STRING
        PropertyType.LONG -> LONG
        PropertyType.BOOL -> BOOLEAN
        else -> null
    }

}
