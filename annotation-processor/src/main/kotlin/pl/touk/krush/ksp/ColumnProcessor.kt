package pl.touk.krush.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import pl.touk.krush.meta.asVariable
import pl.touk.krush.meta.toModelType
import pl.touk.krush.model.*
import pl.touk.krush.validation.ConverterTypeNotFoundException
import pl.touk.krush.validation.EntityNotMappedException
import javax.persistence.AttributeConverter
import javax.persistence.AttributeOverride
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue

class ColumnProcessor(override val resolver: Resolver, private val annEnv: AnnotationEnvironment) : DeclarationProcessor {

    override fun process(graphs: EntityGraphs) {
        processIds(graphs)
        processEmbeddedIds(graphs)
        processColumns(graphs)
        processEmbeddedColumns(graphs)
    }

    private fun processIds(graphs: EntityGraphs) =
        processElements(annEnv.ids, graphs) { entity, (idDecl, _) ->
            val columnAnn: Column? = idDecl.getAnnotationByType(Column::class)
            val columnName = getColumnName(columnAnn, idDecl)
            val converter = getConverterDefinition(idDecl)
            val genValAnn: GeneratedValue? = idDecl.getAnnotationByType(GeneratedValue::class)
            val generatedValue = genValAnn?.let { true } ?: false
            val type = idDecl.toModelType()

            val idPropDef = PropertyDefinition(
                name = idDecl.simpleName.asString(), columnName = columnName, converter = converter,
                column = columnAnn?.let(ColumnDefinition::from), type = type,
                nullable = idDecl.isNullable()
            )
            val idDefinition = IdDefinition(
                name = idDecl.simpleName.asString(), type = type, properties = listOf(idPropDef), generatedValue = generatedValue,
                nullable = idDecl.isNullable(), embedded = false
            )
            entity.copy(id = idDefinition)
        }

    private fun processEmbeddedIds(graphs: EntityGraphs) {
        for (element in annEnv.embeddedIds) { // @Entity Record(@EmbeddedId val id: RecordId)
            val (columnDecl, classDecl) = element
            val embeddableType = columnDecl.toModelType() // RecordId
            val columns = annEnv.embeddableColumns
                .filter { columnElt -> columnElt.second.toModelType() == embeddableType }
                .toList() // id, type
            val entityType = classDecl.toModelType() // Record

            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            graph.computeIfPresent(entityType) { _, entity ->
                //Record
                val columnDefs = columns.map { column -> propertyDefinition(column.first, columnDecl.mappingOverrides()) }
                val idDefinition = IdDefinition(
                    name = columnDecl.simpleName.asString(), type = embeddableType, qualifiedName = embeddableType.qualifiedName,
                    properties = columnDefs, nullable = columnDecl.isNullable(), embedded = true
                )
                entity.copy(id = idDefinition)
            }
        }
    }

    private fun processColumns(graphs: EntityGraphs) =
        processElements(annEnv.columns, graphs) { entity, (columnDecl, _) ->
            val columnDefinition = propertyDefinition(columnDecl)
            entity.addProperty(columnDefinition)
        }

    // TODO: unify with processEmbeddedIds
    private fun processEmbeddedColumns(graphs: EntityGraphs) {
        for (element in annEnv.embeddedColumns) { // @Entity User(@Embedded InvoiceAddress element)
            val (columnDecl, classDecl) = element
            val embeddableType = columnDecl.toModelType() // InvoiceAddress
            val columns = annEnv.embeddableColumns
                .filter { columnElt -> columnElt.second.toModelType() == embeddableType }
                .toList() // city, street, houseNumber
            val entityType = classDecl.toModelType() // User

            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            graph.computeIfPresent(entityType) { _, entity ->
                // User
                val columnDefs = columns.map { column -> propertyDefinition(column.first, columnDecl.mappingOverrides()) }
                val embeddable = EmbeddableDefinition(
                    propertyName = columnDecl.simpleName.asString(), qualifiedName = embeddableType.qualifiedName,
                    nullable = columnDecl.isNullable(), properties = columnDefs
                )
                entity.addEmbeddable(embeddable)
            }
        }
    }


    private fun propertyDefinition(columnDecl: KSPropertyDeclaration, overrideMapping: List<AttributeOverride> = emptyList()): PropertyDefinition {
        val columnAnn: Column? = columnDecl.getAnnotationByType(Column::class)
        val name = columnDecl.simpleName
        val columnName = overrideMapping.overriddenName(name) ?: getColumnName(columnAnn, columnDecl)
        val converter = getConverterDefinition(columnDecl)
        val enumerated = getEnumeratedDefinition(columnDecl)
        val type = columnDecl.toModelType()
        return PropertyDefinition(
            name = name.asString(), columnName = columnName, column = columnAnn?.let(ColumnDefinition::from), type = type,
            nullable = columnDecl.isNullable(), converter = converter, enumerated = enumerated
        )
    }

    private fun getColumnName(columnAnn: Column?, columnElt: KSPropertyDeclaration): String = when {
        columnAnn == null || columnAnn.name.isEmpty() -> columnElt.simpleName.asString()
        else -> columnAnn.name
    }

    private fun Iterable<AttributeOverride>.overriddenName(basicName: KSName): String? =
            this.singleOrNull { it.name == basicName.asVariable() }?.column?.name

    private fun getConverterDefinition(columnDecl: KSPropertyDeclaration): ConverterDefinition? {
        val converterType = columnDecl.getKSAnnotationByType(Convert::class)?.let { convertAnn ->
            convertAnn.arguments.firstOrNull { it.name?.getShortName() == "converter" }?.value as? KSType
        }

        return converterType?.let {
            val qualifiedName = it.toClassName().canonicalName
            val spec = resolver.getClassDeclarationByName(qualifiedName)
            val targetType = spec?.getDeclaredFunctions()
                ?.find { it.simpleName.asString() == AttributeConverter<*, *>::convertToDatabaseColumn.name }
                ?.returnType?.toModelType() ?: throw ConverterTypeNotFoundException(converterType.toClassName().canonicalName)

            return ConverterDefinition(
                name = qualifiedName.asVariable(), targetType = targetType, isObject = spec.classKind == ClassKind.OBJECT
            )
        }
    }

    private fun getEnumeratedDefinition(columnDecl: KSPropertyDeclaration): EnumeratedDefinition? {
        val enumTypeValue = columnDecl.getAnnotationByType(Enumerated::class)?.value
        return enumTypeValue?.let { EnumeratedDefinition(EnumType.valueOf(it.toString())) }
    }

}

private fun KSPropertyDeclaration.isNullable() = this.type.resolve().isMarkedNullable
