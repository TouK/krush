package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Table.PrimaryKey
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.model.*
import pl.touk.krush.validation.*
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

class TablesGenerator : SourceGenerator {

    override fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String, typeEnv: TypeEnvironment): FileSpec {
        val fileSpec = FileSpec.builder(packageName, fileName = "tables")
                .addImport("org.jetbrains.exposed.sql", "Table", "insert")
                .addImport("org.jetbrains.exposed.sql.java-time", "date", "datetime", "timestamp")
                .addImport("pl.touk.krush",
                        "stringWrapper",
                        "longWrapper",
                        "instantWrapper",
                        "zonedDateTime",
                        "booleanWrapper"
                )

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, "${entity.simpleName}Table")
            }
        }

        graph.traverse { entityType, entity ->
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

                val pkType = PrimaryKey::class.asTypeName()
                val pkSpec = PropertySpec.builder("primaryKey", pkType, KModifier.OVERRIDE)
                val pkInitializer = primaryKeyInitializer(id)
                pkSpec.initializer(pkInitializer)
                tableSpec.addProperty(pkSpec.build())

                if (id.converter != null) {
                    val converterName: String = converterFuncName(entityName = entity.name, propertyName = id.name)
                    converterFunc(converterName, typeName, id.converter, fileSpec)
                }

            } ?: throw MissingIdException(entity)

            entity.properties.forEach { column ->
                addTableProperty(column, entity, tableSpec, fileSpec)
            }

            entity.embeddables.forEach { embeddable ->
                embeddable.properties.forEach { prop ->
                    addEmbeddedTableProperty(embeddable, prop, entity, tableSpec, fileSpec, typeEnv)
                }
            }

            entity.getAssociations(AssociationType.MANY_TO_ONE).forEach { assoc ->
                val name = assoc.name.toString()

                val columnType = assoc.targetId.type.asUnderlyingClassName()
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

                val columnType = assoc.targetId.type.asUnderlyingClassName()
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

                val sourceType = entity.id.type.asUnderlyingClassName()
                manyToManyTableSpec.addProperty(
                        PropertySpec.builder("${rootVal}SourceId", Column::class.asClassName().parameterizedBy(sourceType))
                                .initializer(manyToManyPropertyInitializer(entity.id, entity, "_source"))
                                .build()
                )

                val targetIdType = assoc.targetId.type.asUnderlyingClassName()
                val targetEntityDef = graphs.entity(assoc.target.packageName, assoc.target) ?:
                    throw AssociationTargetEntityNotFoundException(assoc.target)
                manyToManyTableSpec.addProperty(
                        PropertySpec.builder("${targetVal}TargetId", Column::class.asClassName().parameterizedBy(targetIdType))
                                .initializer(manyToManyPropertyInitializer(assoc.targetId, targetEntityDef, "_target"))
                                .build()
                )

                fileSpec.addType(manyToManyTableSpec.build())
            }

            insertFunc(entityType = entityType, entity = entity, fileSpec = fileSpec)
        }

        return fileSpec.build()
    }

    private fun addEmbeddedTableProperty(embeddable: EmbeddableDefinition, column: PropertyDefinition, entity: EntityDefinition, tableSpec: TypeSpec.Builder, fileSpec: FileSpec.Builder, typeEnvironment: TypeEnvironment) {
        val name =  typeEnvironment.elementUtils.getName(embeddable.propertyName.asVariable() + column.name.asVariable().capitalize())
        val embeddedProperty = column.copy(name = name)
        addTableProperty(embeddedProperty, entity, tableSpec, fileSpec)
    }

    private fun addTableProperty(column: PropertyDefinition, entity: EntityDefinition, tableSpec: TypeSpec.Builder, fileSpec: FileSpec.Builder) {
        val name = column.name
        val type = column.asTypeName().copy(nullable = column.nullable)
        val columnType = Column::class.asTypeName().parameterizedBy(type)
        val propSpec = PropertySpec.builder(name.asVariable(), columnType)
        val initializer = propertyInitializer(column, entity)

        propSpec.initializer(initializer)
        tableSpec.addProperty(propSpec.build())

        column.converter?.let {
            val converterName: String = converterFuncName(entityName = entity.name, propertyName = column.name)
            converterFunc(converterName, type, it, fileSpec)
        }
    }

    private fun insertFunc(entityType: TypeElement, entity: EntityDefinition, fileSpec: FileSpec.Builder) {
        val entityName = entity.name.asVariable()
        val isGenerated = entity.id?.generatedValue ?: false
        val persistedName = if (isGenerated) "persisted${entityName.capitalize()}" else entityName
        val func = FunSpec.builder("insert")
                .receiver(Type(entityType.packageName, entity.tableName).asUnderlyingClassName())
                .addParameter(entity.name.asVariable(), entityType.asType().asTypeName())
                .returns(entityType.asType().asTypeName())

        val assocParams = entityAssocParams(entity)

        assocParams.forEach { func.addParameter(it) }

        val fromFuncParams = (listOf(entityName) + assocParams.map(ParameterSpec::name)).joinToString(separator = ", ")
        val fromFunc = if (entity.hasAssignableProperties()) "it.from(${fromFuncParams})" else ""

        if (isGenerated) {
            func.addStatement("val id = ${entity.tableName}.insert { $fromFunc }[${entity.tableName}.${entity.id?.name}]")
            func.addStatement("val $persistedName = $entityName.copy(${entity.id?.name} = id)")
        } else {
            func.addStatement("${entity.tableName}.insert { $fromFunc }")
        }

        entity.getAssociations(AssociationType.MANY_TO_MANY).forEach { assoc ->
            val tableName = "${entity.name}${assoc.name.asObject()}Table"
            func.addStatement("$persistedName.${assoc.name}.forEach { ${assoc.name} ->")
            func.addStatement("\t\t${tableName}.insert { it.from($persistedName, ${assoc.name}) }")
            func.addStatement("}\n")
        }

        func.addStatement("return $persistedName")

        fileSpec.addFunction(func.build())
    }

    private fun entityAssocParams(entity: EntityDefinition): List<ParameterSpec> {
        return entity.associations.filter { !it.mapped }.map { assoc ->
            ParameterSpec.builder(
                    assoc.target.simpleName.asVariable(),
                    assoc.target.asType().asTypeName().copy(nullable = true)
            ).defaultValue("null").build()
        }
    }

    private fun converterFunc(name: String, type: TypeName, it: ConverterDefinition, fileSpec: FileSpec.Builder) {
        val wrapperName = when (it.targetType.asUnderlyingClassName()) {
            STRING -> "stringWrapper"
            LONG -> "longWrapper"
            INSTANT -> "instantWrapper"
            BOOLEAN -> "booleanWrapper"
            else -> throw TypeConverterNotSupportedException(it.targetType)
        }

        val converterSpec = FunSpec.builder(name)
                .addParameter("columnName", String::class)
                .receiver(Table::class.java)
                .returns(Column::class.asClassName().parameterizedBy(type))
                .addStatement("return %L<%T>(columnName, { %L().convertToEntityAttribute(it) }, { %L().convertToDatabaseColumn(it) })", wrapperName, type, it.name, it.name)
                .build()
        fileSpec.addFunction(converterSpec)
    }

    private fun idInitializer(id: IdDefinition, entity: EntityDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = if (id.converter != null) {
            converterPropInitializer(entityName = entity.name, propertyName = id.name, columnName = id.columnName.asVariable())
        } else when (id.asUnderlyingTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", id.columnName, id.annotation?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", id.columnName)
            INT -> CodeBlock.of("integer(%S)", id.columnName)
            UUID -> CodeBlock.of("uuid(%S)", id.columnName)
            SHORT -> CodeBlock.of("short(%S)", id.columnName)
            else -> throw IdTypeNotSupportedException(id.type)
        }

        codeBlockBuilder.add(codeBlock)

        if (id.generatedValue) {
            codeBlockBuilder.add(CodeBlock.of(".autoIncrement()")) //TODO disable autoIncrement when id is varchar
        }

        return codeBlockBuilder.build()
    }


    private fun primaryKeyInitializer(id: IdDefinition): CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()
        codeBlockBuilder.add("PrimaryKey(" + id.name + ")")
        return codeBlockBuilder.build()
    }

    private fun propertyInitializer(property: PropertyDefinition, entity: EntityDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = when {
            property.hasConverter() -> converterPropInitializer(entity, property)
            property.isEnumerated() -> enumPropInitializer(property)
            else -> typePropInitializer(property)
        }

        codeBlockBuilder.add(codeBlock)

        if (property.nullable) {
            codeBlockBuilder.add(".nullable()")
        }

        return codeBlockBuilder.build()
    }

    private fun converterPropInitializer(entity: EntityDefinition, property: PropertyDefinition) =
            converterPropInitializer(entityName = entity.name, propertyName = property.name, columnName = property.columnName.asVariable())

    private fun enumPropInitializer(property: PropertyDefinition): CodeBlock {
        val columnName = property.columnName
        val enumType = property.type.asUnderlyingClassName()

        return when (property.enumerated!!.enumType) {
            EnumType.STRING -> {
                val size: Any? = property.annotation?.length ?: 255
                CodeBlock.of("enumerationByName(%S, %L, %L)", columnName, size, "$enumType::class")
            }
            EnumType.ORDINAL -> CodeBlock.of("enumeration(%S, %L)", columnName, "$enumType::class")
        }
    }

    private fun typePropInitializer(property: PropertyDefinition): CodeBlock {
        return when (property.asUnderlyingTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", property.columnName, property.annotation?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", property.columnName)
            BOOLEAN -> CodeBlock.of("bool(%S)", property.columnName)
            UUID -> CodeBlock.of("uuid(%S)", property.columnName)
            INT -> CodeBlock.of("integer(%S)", property.columnName)
            SHORT -> CodeBlock.of("short(%S)", property.columnName)
            FLOAT -> CodeBlock.of("float(%S)", property.columnName)
            DOUBLE -> CodeBlock.of("double(%S)", property.columnName)
            BIG_DECIMAL -> {
                val precision = property.annotation?.precision ?: 0
                val scale = property.annotation?.scale ?: 0
                CodeBlock.of("decimal(%S, %L, %L)", property.columnName, precision, scale)
            }
            LOCAL_DATE -> CodeBlock.of("date(%S)", property.columnName)
            LOCAL_DATE_TIME -> CodeBlock.of("datetime(%S)", property.columnName)
            ZONED_DATE_TIME -> CodeBlock.of("zonedDateTime(%S)", property.columnName)
            INSTANT -> CodeBlock.of("timestamp(%S)", property.columnName)
            else -> throw PropertyTypeNotSupportedExpcetion(property.type)
        }
    }

    private fun associationInitializer(association: AssociationDefinition, idName: String) : CodeBlock {
        val columnName = association.joinColumn ?: "${idName}_${association.targetId.name.asVariable()}"
        val targetTable = "${association.target.simpleName}Table"
        val idCodeBlock = idCodeBlock(association.targetId, association.target.simpleName, columnName)

        return CodeBlock.builder().add(idCodeBlock)
                .add(".references(%L).nullable()", "$targetTable.${association.targetId.name.asVariable()}")
                .build()
    }

    private fun manyToManyPropertyInitializer(id: IdDefinition, entity: EntityDefinition, differentiatior: String) : CodeBlock {
        val columnName = entity.name.asVariable() + differentiatior + "_id"
        val idCodeBlock = idCodeBlock(id, entity.name, columnName)

        return CodeBlock.builder().add(idCodeBlock)
                .add(".references(%L)", "${entity.tableName}.id")
                .build()
    }

    private fun idCodeBlock(id: IdDefinition, entityName: Name, columnName: String): CodeBlock {
        return if (id.converter != null) {
            converterPropInitializer(entityName = entityName, propertyName = id.name, columnName = columnName)
        } else when (id.asUnderlyingTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", columnName, id.annotation?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", columnName)
            INT -> CodeBlock.of("integer(%S)", columnName)
            UUID -> CodeBlock.of("uuid(%S)", columnName)
            SHORT -> CodeBlock.of("short(%S)", columnName)
            else -> throw IdTypeNotSupportedException(id.type)
        }
    }
}

fun IdDefinition.asUnderlyingTypeName(): TypeName {
    return this.type.asUnderlyingClassName()
}

fun PropertyDefinition.asUnderlyingTypeName(): TypeName {
    return this.type.asUnderlyingClassName()
}

fun Type.asUnderlyingClassName(): ClassName {
    return if(this.aliasOf !=null) {
        ClassName(this.aliasOf.packageName, this.aliasOf.simpleName)
    }else{
        ClassName(this.packageName, this.simpleName)
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
@JvmField val LOCAL_DATE =  ClassName("java.time", "LocalDate")
@JvmField val LOCAL_DATE_TIME =  ClassName("java.time", "LocalDateTime")
@JvmField val ZONED_DATE_TIME =  ClassName("java.time", "ZonedDateTime")
@JvmField val INSTANT =  ClassName("java.time", "Instant")
@JvmField val UUID =  ClassName("java.util", "UUID")
