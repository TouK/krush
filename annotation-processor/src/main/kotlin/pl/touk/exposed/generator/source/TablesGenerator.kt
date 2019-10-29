package pl.touk.exposed.generator.source

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
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
import pl.touk.exposed.generator.model.EntityDefinition
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.EntityGraphs
import pl.touk.exposed.generator.model.IdDefinition
import pl.touk.exposed.generator.model.PropertyDefinition
import pl.touk.exposed.generator.model.Type
import pl.touk.exposed.generator.model.allAssociations
import pl.touk.exposed.generator.model.asObject
import pl.touk.exposed.generator.model.asVariable
import pl.touk.exposed.generator.model.entity
import pl.touk.exposed.generator.model.packageName
import pl.touk.exposed.generator.model.traverse
import pl.touk.exposed.generator.validation.AssociationTargetEntityNotFoundException
import pl.touk.exposed.generator.validation.IdTypeNotSupportedException
import pl.touk.exposed.generator.validation.MissingIdException
import pl.touk.exposed.generator.validation.PropertyTypeNotSupportedExpcetion
import pl.touk.exposed.generator.validation.TypeConverterNotSupportedException
import javax.lang.model.element.Name

class TablesGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "tables")
                .addImport("org.jetbrains.exposed.sql", "Table", "datetime")
                .addImport("pl.touk.exposed", "stringWrapper", "longWrapper", "localDateTime", "zonedDateTime")

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
                val name = id.name
                val typeName = ClassName(id.type.packageName, id.type.simpleName)

                val type = Column::class.asTypeName().parameterizedBy(typeName)
                val idSpec = PropertySpec.builder(name.asVariable(), type)
                val builder = CodeBlock.builder()
                val initializer = idInitializer(id, entity)
                builder.add(initializer)

                idSpec.initializer(builder.build())
                tableSpec.addProperty(idSpec.build())

                if (id.converter != null) {
                    val converterName: String = converterFuncName(entityName = entity.name, propertyName = id.name)
                    converterFunc(converterName, typeName, id.converter, fileSpec)
                }

            } ?: throw MissingIdException(entity)

            entity.properties.forEach { column ->
                val name = column.name
                val type = column.asTypeName().copy(nullable =  column.nullable)
                val columnType = Column::class.asTypeName().parameterizedBy(type)
                val propSpec = PropertySpec.builder(name.asVariable(), columnType)
                val initializer = propertyInitializer(column, entity)

                propSpec.initializer(initializer)
                tableSpec.addProperty(propSpec.build())

                if (column.converter != null) {
                    val converterName: String = converterFuncName(entityName = entity.name, propertyName = column.name)
                    converterFunc(converterName, type, column.converter, fileSpec)
                }
            }

            entity.getAssociations(AssociationType.MANY_TO_ONE).forEach { assoc ->
                val name = assoc.name.toString()

                val columnType = assoc.targetId.type.asClassName()
                CodeBlock.builder()
                val initializer = associationInitializer(assoc, name)
                tableSpec.addProperty(
                        PropertySpec.builder(name, Column::class.asClassName().parameterizedBy(columnType.copy(nullable = true)))
                                .initializer(initializer)
                                .build()
                )
            }

            entity.getAssociations(AssociationType.ONE_TO_ONE).filter {it.mapped}.forEach {assoc ->
                val name = assoc.name.toString()

                val columnType = assoc.targetId.type.asClassName()
                CodeBlock.builder()
                val initializer = associationInitializer(assoc, name)
                tableSpec.addProperty(
                        PropertySpec.builder(name, Column::class.asClassName().parameterizedBy(columnType.copy(nullable = true)))
                                .initializer(initializer)
                                .build()
                )
            }

            fileSpec.addType(tableSpec.build())

            entity.getAssociations(AssociationType.MANY_TO_MANY).forEach { assoc ->
                val targetVal = assoc.target.simpleName.asVariable()
                val manyToManyTableName = "${entity.name}${assoc.name.asObject()}Table"
                val manyToManyTableSpec = TypeSpec.objectBuilder(manyToManyTableName)
                        .superclass(Table::class)
                        .addSuperclassConstructorParameter(CodeBlock.of("%S", assoc.joinTable))

                val sourceType = entity.id.type.asClassName()
                manyToManyTableSpec.addProperty(
                        PropertySpec.builder("${rootVal}Id", Column::class.asClassName().parameterizedBy(sourceType))
                                .initializer(manyToManyPropertyInitializer(entity.id, entity))
                                .build()
                )

                val targetIdType = assoc.targetId.type.asClassName()
                val targetEntityDef = graphs.entity(assoc.target.packageName, assoc.target) ?:
                    throw AssociationTargetEntityNotFoundException(assoc.target)
                manyToManyTableSpec.addProperty(
                        PropertySpec.builder("${targetVal}Id", Column::class.asClassName().parameterizedBy(targetIdType))
                                .initializer(manyToManyPropertyInitializer(assoc.targetId, targetEntityDef))
                                .build()
                )

                fileSpec.addType(manyToManyTableSpec.build())
            }
        }

        return fileSpec.build()
    }

    private fun converterFunc(name: String, type: TypeName, it: ConverterDefinition, fileSpec: FileSpec.Builder): FileSpec.Builder {
        val wrapperName = when (it.targetType.asClassName()) {
            STRING -> "stringWrapper"
            LONG -> "longWrapper"
            else -> throw TypeConverterNotSupportedException(it.targetType)
        }

        val converterSpec = FunSpec.builder(name)
                .addParameter("columnName", String::class)
                .receiver(Table::class.java)
                .returns(Column::class.asClassName().parameterizedBy(type))
                .addStatement("return %L<%T>(columnName, { %L().convertToEntityAttribute(it) }, { %L().convertToDatabaseColumn(it) })", wrapperName, type, it.name, it.name)
                .build()
        return fileSpec.addFunction(converterSpec)
    }

    private fun idInitializer(id: IdDefinition, entity: EntityDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = if (id.converter != null) {
            converterPropInitializer(entityName = entity.name, propertyName = id.name, columnName = id.columnName.asVariable())
        } else when (id.asTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", id.columnName, id.annotation?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", id.columnName)
            INT -> CodeBlock.of("integer(%S)", id.columnName)
            UUID -> CodeBlock.of("uuid(%S)", id.columnName)
            SHORT -> CodeBlock.of("short(%S)", id.columnName)
            else -> throw IdTypeNotSupportedException(id.type)
        }

        codeBlockBuilder.add(codeBlock)
        codeBlockBuilder.add(CodeBlock.of(".primaryKey()"))

        if (id.generatedValue) {
            codeBlockBuilder.add(CodeBlock.of(".autoIncrement()")) //TODO disable autoIncrement when id is varchar
        }

        return codeBlockBuilder.build()
    }

    private fun propertyInitializer(property: PropertyDefinition, entity: EntityDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = if (property.converter != null) {
            converterPropInitializer(entityName = entity.name, propertyName = property.name, columnName = property.columnName.asVariable())
        } else when (property.asTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", property.columnName, property.annotation?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", property.columnName)
            BOOLEAN -> CodeBlock.of("bool(%S)", property.columnName)
            DATE_TIME -> CodeBlock.of("datetime(%S)", property.columnName)
            UUID -> CodeBlock.of("uuid(%S)", property.columnName)
            INT -> CodeBlock.of("integer(%S)", property.columnName)
            SHORT -> CodeBlock.of("short(%S)", property.columnName)
            FLOAT -> CodeBlock.of("float(%S)", property.columnName)
            DOUBLE -> CodeBlock.of("double(%S)", property.columnName)
            BIG_DECIMAL -> CodeBlock.of("decimal(%S, %L, %L)", property.columnName, property.annotation?.precision ?: 0, property.annotation?.scale ?: 0)
            LOCAL_DATE_TIME -> CodeBlock.of("localDateTime(%S)", property.columnName)
            ZONED_DATE_TIME -> CodeBlock.of("zonedDateTime(%S)", property.columnName)
            else -> throw PropertyTypeNotSupportedExpcetion(property.type)
        }

        codeBlockBuilder.add(codeBlock)

        if (property.nullable) {
            codeBlockBuilder.add(".nullable()")
        }

        return codeBlockBuilder.build()
    }

    private fun associationInitializer(association: AssociationDefinition, idName: String) : CodeBlock {
        val columnName = association.joinColumn ?: "${idName}_${association.targetId.name.asVariable()}"
        val targetTable = "${association.target.simpleName}Table"
        val idCodeBlock = idCodeBlock(association.targetId, association.target.simpleName, columnName)

        return CodeBlock.builder().add(idCodeBlock)
                .add(".references(%L).nullable()", "$targetTable.${association.targetId.name.asVariable()}")
                .build()
    }

    private fun manyToManyPropertyInitializer(id: IdDefinition, entity: EntityDefinition) : CodeBlock {
        val columnName = entity.name.asVariable() + "_id"
        val idCodeBlock = idCodeBlock(id, entity.name, columnName)

        return CodeBlock.builder().add(idCodeBlock)
                .add(".references(%L)", "${entity.tableName}.id")
                .build()
    }

    private fun idCodeBlock(id: IdDefinition, entityName: Name, columnName: String): CodeBlock {
        return if (id.converter != null) {
            converterPropInitializer(entityName = entityName, propertyName = id.name, columnName = columnName)
        } else when (id.asTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", columnName, id.annotation?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", columnName)
            INT -> CodeBlock.of("integer(%S)", columnName)
            UUID -> CodeBlock.of("uuid(%S)", columnName)
            SHORT -> CodeBlock.of("short(%S)", columnName)
            else -> throw IdTypeNotSupportedException(id.type)
        }
    }
}

fun IdDefinition.asTypeName(): TypeName {
    return this.type.asClassName()
}

fun PropertyDefinition.asTypeName(): TypeName {
    return this.type.asClassName()
}

fun Type.asClassName(): ClassName {
    return ClassName(this.packageName, this.simpleName)
}

private fun converterPropInitializer(entityName: Name, propertyName: Name, columnName: String): CodeBlock {
    val convertFunc = converterFuncName(entityName, propertyName)
    return CodeBlock.of("%L(%S)", convertFunc, columnName)
}

private fun converterFuncName(entityName: Name, propertyName: Name) =
        entityName.asVariable().decapitalize().plus("_$propertyName")

@JvmField val BIG_DECIMAL = ClassName("java.math", "BigDecimal")
@JvmField val LOCAL_DATE_TIME =  ClassName("java.time", "LocalDateTime")
@JvmField val ZONED_DATE_TIME =  ClassName("java.time", "ZonedDateTime")
@JvmField val UUID =  ClassName("java.util", "UUID")
@JvmField val DATE_TIME =  ClassName("org.joda.time", "DateTime")
