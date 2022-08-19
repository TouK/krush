package pl.touk.krush.model

import pl.touk.krush.validation.EntityNotMappedException
import pl.touk.krush.validation.MissingIdException
import javax.persistence.Column
import javax.persistence.JoinColumn

data class EntityDefinition(
    val type: Type,
    val table: String,
    val id: IdDefinition? = null,
    val properties: List<PropertyDefinition> = emptyList(),
    val associations: List<AssociationDefinition> = emptyList(),
    val embeddables: List<EmbeddableDefinition> = emptyList()
) {
    fun addProperty(column: PropertyDefinition) = this.copy(properties = this.properties + column)

    fun addAssociation(association: AssociationDefinition) = this.copy(associations = this.associations + association)

    fun addEmbeddable(embeddable: EmbeddableDefinition) = this.copy(embeddables = this.embeddables + embeddable)

    fun getPropertyNames() = properties.map(PropertyDefinition::name)

    fun getAssociations(vararg types: AssociationType) = associations.filter { it.type in types }

    fun hasAssignableProperties(): Boolean {
       return id?.generatedValue == false || properties.isNotEmpty() || embeddables.isNotEmpty()
               || getAssociations(AssociationType.MANY_TO_MANY).isNotEmpty()
               || getAssociations(AssociationType.ONE_TO_ONE).any { it.mapped }
    }

    fun hasSelfReferentialAssoc(): Boolean = associations.any { it.isSelfReferential }

    val name: String get() = type.simpleName
    val qualifiedName: String get() = type.qualifiedName

    val tableName: String get() = "${name}Table"
    val idColumn: String get() = id?.let { id -> "${tableName}.${id.name}" } ?: throw MissingIdException(this)
}

data class IdDefinition (
    val name: String,
    val type: Type,
    // for composite id
    val qualifiedName: String? = null,
    // for non-composite id just single property
    val properties: List<PropertyDefinition> = emptyList(),
    val generatedValue: Boolean = false,
    val nullable: Boolean,
    val embedded: Boolean = false,
    val sharedAssoc: AssociationDefinition? = null
) {

    fun propName(prop: PropertyDefinition): String {
        return if (embedded) {
            val idTypeName = type.simpleName.decapitalize()
            "$idTypeName${prop.name.asVariable().capitalize()}"
        } else name.asVariable()
    }

    val propsAsArgs: String get() = this.properties.map { this.propName(it) }.joinToString(", ")
}

data class AssociationDefinition(
    val name: String,
    val source: Type,
    val target: Type,
    val mapped: Boolean = true,
    val mappedBy: String? = null,
    val joinColumns: List<JoinColumn> = emptyList(),
    val joinTable: String? = null,
    val nullable: Boolean = false,
    val type: AssociationType,
    val targetId: IdDefinition,
    val sharedId: IdDefinition? = null
) {
    val targetTable: String get() = "${target.simpleName}Table"

    fun targetIdPropName(targetIdProp: PropertyDefinition) =
        "${name.asVariable()}${targetIdProp.valName.capitalize()}"

    fun defaultIdPropName() = targetIdPropName(targetId.properties[0])

    val isSelfReferential get() = source == target

    val isBidirectional get() = mappedBy != null
}

data class ColumnDefinition(
    val name: String,
    val length: Int,
    val precision: Int = 0,
    val scale: Int = 0,
    val isJsonb: Boolean = false
) {
    companion object {
        fun from(column: Column) = ColumnDefinition(
            name = column.name, length = column.length,
            precision = column.precision, scale = column.scale,
            isJsonb = column.columnDefinition.toLowerCase() == "jsonb"
        )
    }
}

data class PropertyDefinition(
    val name: String,
    val columnName: String,
    val column: ColumnDefinition?,
    val sharedColumn: JoinColumn? = null,
    val type: Type,
    val nullable: Boolean,
    val converter: ConverterDefinition? = null,
    val enumerated: EnumeratedDefinition? = null,
) {
    val valName: String get() = name.asVariable()

    fun hasConverter(): Boolean {
        return converter != null
    }

    fun isEnumerated(): Boolean {
        return enumerated != null
    }

    fun isJsonb() = column?.isJsonb ?: false
}

data class ConverterDefinition(
    val name: String,
    val targetType: Type,
    val isObject: Boolean = false
)

data class EnumeratedDefinition(
    val enumType: EnumType
)

enum class EnumType {
    STRING, ORDINAL
}

data class Type(
    val packageName: String,
    val simpleName: String,
    val aliasOf: Type? = null
) {
    val qualifiedName: String by lazy { if (packageName.isNotBlank()) "$packageName.$simpleName" else simpleName }
}

data class EmbeddableDefinition(
    val propertyName: String,
    val qualifiedName: String,
    val nullable: Boolean,
    val properties: List<PropertyDefinition> = emptyList()
) {
    fun getPropertyNames() = properties.map(PropertyDefinition::name)
}

enum class AssociationType {
    ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
}

typealias EntityGraphs = MutableMap<String, EntityGraph>
typealias EntityGraph = MutableMap<Type, EntityDefinition>

fun EntityGraph(): EntityGraph = mutableMapOf()
fun EntityGraphs(): EntityGraphs = mutableMapOf()

fun EntityGraph.traverse(function: (EntityDefinition) -> Unit) {
    this.entries.forEach { (_, value) -> function.invoke(value) }
}

fun EntityGraph.traverse(function: (Type, EntityDefinition) -> Unit) {
    this.entries.forEach { (key, value) -> function.invoke(key, value) }
}

class DFS(val graphs: EntityGraphs) {
    private val result = mutableSetOf<EntityDefinition>()
    private val visited = mutableSetOf<Type>()

    fun visit(elem: Type): List<EntityDefinition> {
        val current = graphs.entity(elem.packageName, elem) ?: throw EntityNotMappedException(elem)
        result.add(current)
        visited.add(elem)
        val remaining = current.associations.map { it.target }.filterNot { visited.contains(it) }
        remaining.forEach { visit(it) }
        return result.toList()
    }
}

fun EntityGraph.allAssociations() =
        this.values.flatMap { entityDef -> entityDef.associations.map { it.target } }.toSet()

fun EntityGraphs.entityId(type: Type) : IdDefinition {
    val graph = this[type.packageName] ?: throw EntityNotMappedException(type)
    return graph[type]?.id ?: throw EntityNotMappedException(type)
}

fun EntityGraphs.entity(packageName: String, type: Type) : EntityDefinition? {
    return this[packageName]?.get(type)
}

fun EntityGraphs.entities() : Iterable<EntityDefinition> = this.map { it.value }.flatMap { it.entries }.map { it.value }

fun String.asObject() = this.capitalize()
fun String.asVariable() = this.decapitalize()
