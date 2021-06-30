package pl.touk.krush.source

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Table.PrimaryKey
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.model.*
import pl.touk.krush.meta.toClassName
import pl.touk.krush.validation.*
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
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

        val isJsonbUsed = graph.any { (_, entity) ->
            entity.properties.any { it.column?.columnDefinition == "jsonb" }
        }

        if (isJsonbUsed) {
            fileSpec.addImport("pl.touk.krush", "jsonb")
        }

        graph.allAssociations().forEach { entity ->
            if (entity.packageName != packageName) {
                fileSpec.addImport(entity.packageName, "${entity.simpleName}Table")
            }
        }

        graph.traverse { entityType, entity ->
            val tableSpec = TypeSpec.objectBuilder("${entity.name}Table")
                    .superclass(Table::class)
                    .addSuperclassConstructorParameter(CodeBlock.of("%S", entity.table))

            entity.id?.let { id ->
                id.properties.forEach { prop ->
                    val typeName = ClassName(prop.type.packageName, prop.type.simpleName)

                    val type = Column::class.asTypeName().parameterizedBy(typeName)
                    val idSpec = PropertySpec.builder(id.propName(prop), type)
                    val builder = CodeBlock.builder()
                    val initializer = idInitializer(id, prop, entity)
                    builder.add(initializer)

                    idSpec.initializer(builder.build())
                    tableSpec.addProperty(idSpec.build())

                    if (prop.converter != null) {
                        val converterName: String = converterFuncName(entityName = entity.name, propertyName = id.name)
                        converterFunc(converterName, typeName, prop.converter, fileSpec)
                    }
                }

                val pkType = PrimaryKey::class.asTypeName()
                val pkSpec = PropertySpec.builder("primaryKey", pkType, KModifier.OVERRIDE)
                val pkInitializer = primaryKeyInitializer(id)
                pkSpec.initializer(pkInitializer)
                tableSpec.addProperty(pkSpec.build())

            } ?: throw MissingIdException(entity)

            entity.properties.forEach { property ->
                addTableProperty(property, entity, tableSpec, fileSpec)
            }

            entity.embeddables.forEach { embeddable ->
                embeddable.properties.forEach { prop ->
                    addEmbeddedTableProperty(embeddable, prop, entity, tableSpec, fileSpec, typeEnv)
                }
            }

            entity.getAssociations(AssociationType.MANY_TO_ONE).filter { it.sharedId == null }.forEach { assoc ->
                addAssociationProperty(assoc, tableSpec)
            }

            entity.getAssociations(AssociationType.ONE_TO_ONE).filter { it.mapped && it.sharedId == null }.forEach { assoc ->
                addAssociationProperty(assoc, tableSpec)
            }

            fileSpec.addType(tableSpec.build())

            entity.getAssociations(AssociationType.MANY_TO_MANY).forEach { assoc ->
                val targetEntity =
                    graphs.entity(assoc.target.packageName, assoc.target) ?: throw AssociationTargetEntityNotFoundException(assoc.target)
                addManyToManyAssociationTable(assoc, entity, entity.id, targetEntity, fileSpec)
            }

            addInsertFunc(entityType, entity, fileSpec)
        }

        return fileSpec.build()
    }

    private fun addAssociationProperty(assoc: AssociationDefinition, tableSpec: TypeSpec.Builder) {
        assoc.targetId.properties.forEach { targetIdProp ->
            val name = assoc.targetIdPropName(targetIdProp)
            val columnType = targetIdProp.type.asUnderlyingClassName()
            CodeBlock.builder()
            val initializer = associationInitializer(assoc, targetIdProp, name)
            tableSpec.addProperty(
                PropertySpec.builder(name, Column::class.asClassName().parameterizedBy(columnType.copy(nullable = true)))
                    .initializer(initializer)
                    .build()
            )
        }
    }

    private fun addManyToManyAssociationTable(
        assoc: AssociationDefinition, entity: EntityDefinition, id: IdDefinition, targetEntity: EntityDefinition, fileSpec: FileSpec.Builder
    ) {
        val manyToManyTableName = "${entity.name}${assoc.name.asObject()}Table"
        val manyToManyTableSpec = TypeSpec.objectBuilder(manyToManyTableName)
            .superclass(Table::class)
            .addSuperclassConstructorParameter(CodeBlock.of("%S", assoc.joinTable))

        id.properties.forEach { sourceIdProp ->
            val (prefix, differentiator) = if (assoc.isSelfReferential) "Source" to "_source" else "" to ""
            val sourceType = sourceIdProp.type.asUnderlyingClassName()
            val rootVal = entity.name.asVariable()
            val propName = "${rootVal}${prefix}${sourceIdProp.valName.capitalize()}"
            manyToManyTableSpec.addProperty(
                PropertySpec.builder(propName, Column::class.asClassName().parameterizedBy(sourceType))
                    .initializer(manyToManyPropertyInitializer(id, sourceIdProp, entity, differentiator))
                    .build()
            )
        }

        assoc.targetId.properties.forEach { targetIdProp ->
            val (prefix, differentiator) = if (assoc.isSelfReferential) "Target" to "_target" else "" to ""
            val targetIdType = targetIdProp.type.asUnderlyingClassName()
            val rootVal = targetEntity.name.asVariable()
            val propName = "${rootVal}${prefix}${targetIdProp.valName.capitalize()}"
            manyToManyTableSpec.addProperty(
                PropertySpec.builder(propName, Column::class.asClassName().parameterizedBy(targetIdType))
                    .initializer(manyToManyPropertyInitializer(assoc.targetId, targetIdProp, targetEntity, differentiator))
                    .build()
            )
        }

        fileSpec.addType(manyToManyTableSpec.build())
    }

    private fun addEmbeddedTableProperty(embeddable: EmbeddableDefinition, column: PropertyDefinition, entity: EntityDefinition, tableSpec: TypeSpec.Builder, fileSpec: FileSpec.Builder, typeEnvironment: TypeEnvironment) {
        val name = typeEnvironment.elementUtils.getName(embeddable.propertyName.asVariable() + column.name.asVariable().capitalize())
        val embeddedProperty = column.copy(name = name, nullable = embeddable.nullable || column.nullable)
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

    private fun addInsertFunc(entityType: TypeElement, entity: EntityDefinition, fileSpec: FileSpec.Builder) {
        val entityName = entity.name.asVariable()
        val isGenerated = entity.id?.generatedValue ?: false
        val persistedName = if (isGenerated) "persisted${entityName.capitalize()}" else entityName
        val entityClass = entityType.toImmutableKmClass().toClassName()
        val func = FunSpec.builder("insert")
                .receiver(Type(entityType.packageName, entity.tableName).asUnderlyingClassName())
                .addParameter(entity.name.asVariable(), entityClass)
                .returns(entityClass)

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
                    assoc.target.simpleName.asVariable() +"Param",
                    assoc.target.toImmutableKmClass().toClassName().copy(nullable = true)
            ).defaultValue("null").build()
        }
    }

    private fun converterFunc(name: String, type: TypeName, converter: ConverterDefinition, fileSpec: FileSpec.Builder) {
        val wrapperName = when (converter.targetType.asUnderlyingClassName()) {
            STRING -> "stringWrapper"
            LONG -> "longWrapper"
            INSTANT -> "instantWrapper"
            BOOLEAN -> "booleanWrapper"
            else -> throw TypeConverterNotSupportedException(converter.targetType)
        }
        val init = if (!converter.isObject) "()" else ""

        val converterSpec = FunSpec.builder(name)
                .addParameter("columnName", String::class)
                .receiver(Table::class.java)
                .returns(Column::class.asClassName().parameterizedBy(type))
                .addStatement("return %L<%T>(columnName, { %L$init.convertToEntityAttribute(it) }, { %L$init.convertToDatabaseColumn(it) })", wrapperName, type, converter.name, converter.name)
                .build()
        fileSpec.addFunction(converterSpec)
    }

    private fun idInitializer(id: IdDefinition, prop: PropertyDefinition, entity: EntityDefinition) : CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        val codeBlock = if (prop.converter != null) {
            converterPropInitializer(entityName = entity.name, propertyName = prop.name, columnName = prop.columnName.asVariable())
        } else when (prop.asUnderlyingTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", prop.columnName, prop.column?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", prop.columnName)
            INT -> CodeBlock.of("integer(%S)", prop.columnName)
            UUID -> CodeBlock.of("uuid(%S)", prop.columnName)
            SHORT -> CodeBlock.of("short(%S)", prop.columnName)
            else -> {
                if (prop.isEnumerated()) {
                    enumPropInitializer(prop)
                } else {
                    throw IdTypeNotSupportedException(prop.type)
                }
            }
        }

        codeBlockBuilder.add(codeBlock)

        if (id.generatedValue) {
            codeBlockBuilder.add(CodeBlock.of(".autoIncrement()")) //TODO disable autoIncrement when id is varchar
        }

        if (id.sharedAssoc != null && prop.sharedColumn != null) {
            val targetIdProp = id.sharedAssoc.targetId.properties.find { it.columnName.toString() == prop.sharedColumn.name }
            targetIdProp?.let {
                codeBlockBuilder
                    .add(".references(%L)", "${id.sharedAssoc.targetTable}.${id.sharedAssoc.targetId.propName(targetIdProp)}")
                    .build()
            }
        }

        return codeBlockBuilder.build()
    }

    private fun primaryKeyInitializer(id: IdDefinition): CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()
        codeBlockBuilder.add("PrimaryKey(" + id.properties.joinToString(", ") { id.propName(it) } + ")")
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
                val size: Int = property.column?.length ?: 255
                CodeBlock.of("enumerationByName(%S, %L, %L)", columnName, size, "$enumType::class")
            }
            EnumType.ORDINAL -> CodeBlock.of("enumeration(%S, %L)", columnName, "$enumType::class")
        }
    }

    private fun typePropInitializer(property: PropertyDefinition): CodeBlock {
        return when (property.asUnderlyingTypeName()) {
            STRING -> when {
                property.isJsonb() -> CodeBlock.of("jsonb(%S)", property.columnName)
                else -> CodeBlock.of("varchar(%S, %L)", property.columnName, property.column?.length ?: 255)
            }
            LONG -> CodeBlock.of("long(%S)", property.columnName)
            BOOLEAN -> CodeBlock.of("bool(%S)", property.columnName)
            UUID -> CodeBlock.of("uuid(%S)", property.columnName)
            INT -> CodeBlock.of("integer(%S)", property.columnName)
            SHORT -> CodeBlock.of("short(%S)", property.columnName)
            FLOAT -> CodeBlock.of("float(%S)", property.columnName)
            DOUBLE -> CodeBlock.of("double(%S)", property.columnName)
            BIG_DECIMAL -> {
                val precision = property.column?.precision ?: 0
                val scale = property.column?.scale ?: 0
                CodeBlock.of("decimal(%S, %L, %L)", property.columnName, precision, scale)
            }
            LOCAL_DATE -> CodeBlock.of("date(%S)", property.columnName)
            LOCAL_DATE_TIME -> CodeBlock.of("datetime(%S)", property.columnName)
            ZONED_DATE_TIME -> CodeBlock.of("zonedDateTime(%S)", property.columnName)
            INSTANT -> CodeBlock.of("timestamp(%S)", property.columnName)
            else -> throw PropertyTypeNotSupportedException(property.type)
        }
    }

    private fun associationInitializer(assoc: AssociationDefinition, idProp: PropertyDefinition, idName: String) : CodeBlock {
        val columnName = assoc.joinColumns.find { it.name == idProp.columnName.toString() }?.name
            ?: "${idName}_${assoc.targetId.name.asVariable()}"
        val idCodeBlock = idCodeBlock(idProp, assoc.target.simpleName, columnName)

        return CodeBlock.builder().add(idCodeBlock)
            .add(".references(%L).nullable()", "${assoc.targetTable}.${assoc.targetId.propName(idProp)}")
            .build()
    }

    private fun manyToManyPropertyInitializer(
        id: IdDefinition, idProp: PropertyDefinition, entity: EntityDefinition, differentiator: String
    ) : CodeBlock {
        val targetTable = entity.tableName
        val columnName = entity.name.asVariable() + differentiator + "_" + idProp.columnName.toString()
        val idCodeBlock = idCodeBlock(idProp, entity.name, columnName)

        return CodeBlock.builder().add(idCodeBlock)
                .add(".references(%L)", "$targetTable.${id.propName(idProp)}")
                .build()
    }

    private fun idCodeBlock(prop: PropertyDefinition, entityName: Name, columnName: String): CodeBlock {
        return if (prop.converter != null) {
            converterPropInitializer(entityName = entityName, propertyName = prop.name, columnName = columnName)
        } else when (prop.asUnderlyingTypeName()) {
            STRING -> CodeBlock.of("varchar(%S, %L)", columnName, prop.column?.length ?: 255)
            LONG -> CodeBlock.of("long(%S)", columnName)
            INT -> CodeBlock.of("integer(%S)", columnName)
            UUID -> CodeBlock.of("uuid(%S)", columnName)
            SHORT -> CodeBlock.of("short(%S)", columnName)
            else -> throw IdTypeNotSupportedException(prop.type)
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
