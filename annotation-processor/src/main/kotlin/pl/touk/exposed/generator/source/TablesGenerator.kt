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
import pl.touk.exposed.generator.model.IdType
import pl.touk.exposed.generator.model.PropertyType
import pl.touk.exposed.generator.model.allAssociations
import pl.touk.exposed.generator.model.asObject
import pl.touk.exposed.generator.model.asVariable
import pl.touk.exposed.generator.model.packageName
import pl.touk.exposed.generator.model.traverse
import java.util.UUID

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
            val rootVal = entity.name.asVariable()

            val tableSpec = TypeSpec.objectBuilder("${entity.name}Table")
                    .superclass(Table::class)
                    .addSuperclassConstructorParameter(CodeBlock.of("%S", entity.table))

            entity.id?.let { id ->
                val name = id.name.toString()

                val columnType = Column::class.asTypeName().parameterizedBy(id.type.asTypeName() ?: id.typeMirror.asTypeName())
                val idSpec = PropertySpec.builder(name, columnType)
                val builder = CodeBlock.builder()
                val initializer = when (id.type) {
                    IdType.STRING ->  CodeBlock.of("varchar(%S, %L)", name, id.annotation?.length ?: 255)
                    IdType.LONG -> CodeBlock.of("long(%S)", name)
                    IdType.INTEGER -> CodeBlock.of("integer(%S)", name)
                    IdType.UUID -> CodeBlock.of("uuid(%S)", name)
                    IdType.SHORT -> CodeBlock.of("short(%S)", name)
                }
                builder.add(initializer)

                if (id.generatedValue) {
                    builder.add(CodeBlock.of(".autoIncrement()")) //TODO disable autoIncrement when id is varchar
                }
                idSpec.initializer(builder.build())
                tableSpec.addProperty(idSpec.build())
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
                    PropertyType.UUID -> CodeBlock.of("uuid(%S)", name)
                }
                propSpec.initializer(initializer)
                tableSpec.addProperty(propSpec.build())
            }

            entity.getAssociations(AssociationType.MANY_TO_ONE).forEach { assoc ->
                val name = assoc.name.toString()
                val columnName = assoc.joinColumn ?: "${name}_id"
                val targetTable = "${assoc.target.simpleName}Table"
                val columnType = assoc.idType.asTypeName()
                val initializer = when (assoc.idType) {
                    IdType.STRING ->  CodeBlock.of("varchar(%S, %L).references(%L).nullable()", columnName, 255, "$targetTable.id") //todo read length from annotation
                    IdType.LONG -> CodeBlock.of("long(%S).references(%L).nullable()", columnName, "$targetTable.id")
                    IdType.INTEGER -> CodeBlock.of("integer(%S).references(%L).nullable()", name, "$targetTable.id")
                    IdType.UUID -> CodeBlock.of("uuid(%S).references(%L).nullable()", name, "$targetTable.id")
                    IdType.SHORT -> CodeBlock.of("short(%S).references(%L).nullable()", name, "$targetTable.id")
                }
                tableSpec.addProperty(
                        PropertySpec.builder(name, Column::class.asClassName().parameterizedBy(columnType?.copy(nullable = true) ?: UUID::class.java.asTypeName()))
                                .initializer(initializer)
                                .build()
                )
            }

            fileSpec.addType(tableSpec.build())

            entity.getAssociations(AssociationType.MANY_TO_MANY).forEach { assoc ->
                val targetVal = assoc.target.simpleName.asVariable()
                val targetTable = "${assoc.target.simpleName}Table"
                val manyToManyTableName = "${entity.name}${assoc.name.asObject()}Table"
                val manyToManyTableSpec = TypeSpec.objectBuilder(manyToManyTableName)
                        .superclass(Table::class)
                        .addSuperclassConstructorParameter(CodeBlock.of("%S", assoc.joinTable))

                manyToManyTableSpec.addProperty(
                        PropertySpec.builder("${rootVal}Id", Column::class.java.parameterizedBy(Long::class.java))
                                .initializer("long(\"${rootVal}_id\").references(${entity.tableName}.id)")
                                .build()
                )
                manyToManyTableSpec.addProperty(
                        PropertySpec.builder("${targetVal}Id", Column::class.java.parameterizedBy(Long::class.java))
                                .initializer("long(\"${targetVal}_id\").references($targetTable.id)")
                                .build()
                )

                fileSpec.addType(manyToManyTableSpec.build())
            }
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

