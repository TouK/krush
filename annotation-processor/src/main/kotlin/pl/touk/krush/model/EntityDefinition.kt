package pl.touk.krush.model

import pl.touk.krush.validation.EntityNotMappedException
import pl.touk.krush.validation.MissingIdException
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.Table

data class EntityDefinition(
    val type: TypeElement,
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

    val name: Name get() = type.simpleName
    val qualifiedName: Name get() = type.qualifiedName

    val tableName: String get() = "${name}Table"
    val idColumn: String get() = id?.let { id -> "${tableName}.${id.name}" } ?: throw MissingIdException(this)
}

data class IdDefinition (
    val name: Name,
    val type: Type,
    // for composite id
    val qualifiedName: Name? = null,
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
    val name: Name,
    val source: TypeElement,
    val target: TypeElement,
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
}

data class PropertyDefinition(
    val name: Name,
    val columnName: Name,
    val column: Column?,
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

    fun isJsonb() = column?.columnDefinition?.toLowerCase() == "jsonb"
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
)

data class EmbeddableDefinition(
    val propertyName: Name,
    val qualifiedName: Name,
    val nullable: Boolean,
    val properties: List<PropertyDefinition> = emptyList()
) {
    fun getPropertyNames() = properties.map(PropertyDefinition::name)
}

enum class AssociationType {
    ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
}

typealias EntityGraphs = MutableMap<String, EntityGraph>
typealias EntityGraph = MutableMap<TypeElement, EntityDefinition>

fun EntityGraph(): EntityGraph = mutableMapOf()
fun EntityGraphs(): EntityGraphs = mutableMapOf()

fun EntityGraph.traverse(function: (EntityDefinition) -> Unit) {
    this.entries.forEach { (_, value) -> function.invoke(value) }
}

fun EntityGraph.traverse(function: (TypeElement, EntityDefinition) -> Unit) {
    this.entries.forEach { (key, value) -> function.invoke(key, value) }
}

fun EntityGraph.allAssociations() =
        this.values.flatMap { entityDef -> entityDef.associations.map { it.target } }.toSet()

fun EntityGraphs.entityId(typeElement: TypeElement) : IdDefinition {
    val graph = this[typeElement.packageName] ?: throw EntityNotMappedException(typeElement)
    return graph[typeElement]?.id ?: throw EntityNotMappedException(typeElement)
}

fun EntityGraphs.entity(packageName: String, typeElement: TypeElement) : EntityDefinition? {
    return this[packageName]?.get(typeElement)
}

fun EntityGraphs.entities() : Iterable<EntityDefinition> = this.map { it.value }.flatMap { it.entries }.map { it.value }

fun Name.asObject() = this.toString().capitalize()
fun Name.asVariable() = this.toString().decapitalize()

val TypeElement.packageName: String
    get() {
        val dotIdx = this.qualifiedName.lastIndexOf('.')
        if (dotIdx < 0) {
            return "default"
        }
        return this.qualifiedName.substring(0 until dotIdx)
    }

val TypeElement.tableName: String
    get() {
        return this.getAnnotation(Table::class.java)?.name ?: this.simpleName.asVariable()
    }
