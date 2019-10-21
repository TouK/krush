package pl.touk.exposed.generator.source

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import pl.touk.exposed.generator.model.AssociationDefinition
import pl.touk.exposed.generator.model.AssociationType
import pl.touk.exposed.generator.model.ConverterDefinition
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.EntityGraphs
import pl.touk.exposed.generator.model.IdDefinition
import pl.touk.exposed.generator.model.IdType
import pl.touk.exposed.generator.model.PropertyDefinition
import pl.touk.exposed.generator.model.PropertyType
import pl.touk.exposed.generator.model.TypeWrapperTargetType
import pl.touk.exposed.generator.model.allAssociations
import pl.touk.exposed.generator.model.asObject
import pl.touk.exposed.generator.model.asVariable
import pl.touk.exposed.generator.model.packageName
import pl.touk.exposed.generator.model.traverse
import javax.lang.model.type.DeclaredType

class TablesGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "tables")
                .addImport("org.jetbrains.exposed.sql", "Table")
                .addImport("pl.touk.exposed", "stringWrapper", "longWrapper")

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
                val initializer = createIdInitializer(id)
                builder.add(initializer)

                idSpec.initializer(builder.build())
                tableSpec.addProperty(idSpec.build())
            }

            entity.properties.forEach { column ->
                val name = column.name.toString()
                val type = column.type.asTypeName()?.copy(nullable =  column.nullable) ?: column.typeMirror.asTypeName()
                val columnType = Column::class.asTypeName().parameterizedBy(type)
                val propSpec = PropertySpec.builder(name, columnType)
                val initializer = createPropertyInitializer(column)

                propSpec.initializer(initializer)
                tableSpec.addProperty(propSpec.build())

                column.converter?.let {
                    createColumnConverter(name, type, column, it, fileSpec)
                }
            }

            entity.getAssociations(AssociationType.MANY_TO_ONE).forEach { assoc ->
                val name = assoc.name.toString()

                val columnType = assoc.targetId.type.asTypeName() ?: assoc.targetId.typeMirror.asTypeName()
                CodeBlock.builder()
                val initializer = createAssociationInitializer(assoc, name)
                tableSpec.addProperty(
                        PropertySpec.builder(name, Column::class.asClassName().parameterizedBy(columnType.copy(nullable = true)))
                                .initializer(initializer)
                                .build()
                )
            }

            entity.getAssociations(AssociationType.ONE_TO_ONE).filter {it.mapped}.forEach {assoc ->
                val name = assoc.name.toString()

                val columnType = assoc.targetId.type.asTypeName() ?: assoc.targetId.typeMirror.asTypeName()
                CodeBlock.builder()
                val initializer = createAssociationInitializer(assoc, name)
                tableSpec.addProperty(
                        PropertySpec.builder(name, Column::class.asClassName().parameterizedBy(columnType.copy(nullable = true)))
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

    private fun createColumnConverter(name: String, type: TypeName, column: PropertyDefinition, it: ConverterDefinition, fileSpec: FileSpec.Builder): FileSpec.Builder {
        val wrapperName = when (it.typeWrapper) {
            TypeWrapperTargetType.STRING -> "stringWrapper"
            TypeWrapperTargetType.LONG -> "longWrapper"
        }

        val converterSpec = FunSpec.builder(name)
                .addParameter("columnName", String::class)
                .receiver(Table::class.java)
                .returns(Column::class.asClassName().parameterizedBy(type))
                .addStatement("return %L<%T>(columnName, { %L().convertToEntityAttribute(it) }, { %L().convertToDatabaseColumn(it) })", wrapperName, column.typeMirror, it.name, it.name)
                .build()
        return fileSpec.addFunction(converterSpec)
    }

    private fun createIdInitializer(id: IdDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = when (id.type) {
            IdType.STRING -> CodeBlock.of("varchar(%S, %L)", id.columnName, id.annotation?.length ?: 255)
            IdType.LONG -> CodeBlock.of("long(%S)", id.columnName)
            IdType.INTEGER -> CodeBlock.of("integer(%S)", id.columnName)
            IdType.UUID -> CodeBlock.of("uuid(%S)", id.columnName)
            IdType.SHORT -> CodeBlock.of("short(%S)", id.columnName)
        }

        codeBlockBuilder.add(codeBlock)
        codeBlockBuilder.add(CodeBlock.of(".primaryKey()"))

        if (id.generatedValue) {
            codeBlockBuilder.add(CodeBlock.of(".autoIncrement()")) //TODO disable autoIncrement when id is varchar
        }

        return codeBlockBuilder.build()
    }

    private fun createPropertyInitializer(property: PropertyDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = if (property.converter != null) {
            val convertFunc = (property.typeMirror as DeclaredType).asElement().simpleName.toString().decapitalize()
            CodeBlock.of("%L(%S)", convertFunc, property.name)
        } else when (property.type) {
            PropertyType.STRING -> CodeBlock.of("varchar(%S, %L)", property.columnName, property.annotation?.length ?: 255)
            PropertyType.LONG -> CodeBlock.of("long(%S)", property.columnName)
            PropertyType.BOOL -> CodeBlock.of("bool(%S)", property.columnName)
            PropertyType.DATE_TIME -> CodeBlock.of("datetime(%S)", property.columnName)
            PropertyType.UUID -> CodeBlock.of("uuid(%S)", property.columnName)
            PropertyType.INTEGER -> CodeBlock.of("integer(%S)", property.columnName)
            PropertyType.SHORT -> CodeBlock.of("short(%S)", property.columnName)
            PropertyType.FLOAT -> CodeBlock.of("float(%S)", property.columnName)
            PropertyType.DOUBLE -> CodeBlock.of("double(%S)", property.columnName)
            PropertyType.BIG_DECIMAL -> CodeBlock.of("decimal(%S, %L, %L)", property.columnName, property.annotation?.precision ?: 0, property.annotation?.scale ?: 0)
            else -> TODO()
        }

        codeBlockBuilder.add(codeBlock)

        if (property.nullable) {
            codeBlockBuilder.add(".nullable()")
        }

        return codeBlockBuilder.build()
    }

    private fun createAssociationInitializer(association: AssociationDefinition, idName: String) : CodeBlock {
        val columnName = association.joinColumn ?: "${idName}_${association.targetId.name.asVariable()}"
        val targetTable = "${association.target.simpleName}Table"

        val codeBlockBuilder = CodeBlock.builder()
        when (association.targetId.type) {
            IdType.STRING -> codeBlockBuilder.add(CodeBlock.of("varchar(%S, %L)", columnName, 255)) //todo read length from annotation
            IdType.LONG -> codeBlockBuilder.add(CodeBlock.of("long(%S)", columnName))
            IdType.INTEGER -> codeBlockBuilder.add(CodeBlock.of("integer(%S)", columnName))
            IdType.UUID -> codeBlockBuilder.add(CodeBlock.of("uuid(%S)", columnName))
            IdType.SHORT -> codeBlockBuilder.add(CodeBlock.of("short(%S)", columnName))
        }

        return codeBlockBuilder.add(".references(%L).nullable()", "$targetTable.${association.targetId.name.asVariable()}").build()
    }
}

private fun PropertyType.asTypeName(): TypeName? {
    return when (this) {
        PropertyType.STRING -> STRING
        PropertyType.LONG -> LONG
        PropertyType.BOOL -> BOOLEAN
        PropertyType.INTEGER -> INT
        PropertyType.SHORT -> SHORT
        PropertyType.FLOAT -> FLOAT
        PropertyType.DOUBLE -> DOUBLE
        else -> null
    }
}

fun IdType.asTypeName(): TypeName? {
    return when (this) {
        IdType.LONG -> com.squareup.kotlinpoet.LONG
        IdType.STRING -> com.squareup.kotlinpoet.STRING
        IdType.INTEGER -> com.squareup.kotlinpoet.INT
        IdType.SHORT -> com.squareup.kotlinpoet.SHORT
        else -> null
    }
}
