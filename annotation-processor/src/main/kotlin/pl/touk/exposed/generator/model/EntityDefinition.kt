package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.validation.EntityNotMappedException
import pl.touk.exposed.generator.validation.MissingIdException
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.persistence.Column
import javax.persistence.Table

data class EntityDefinition(
        val name: Name,
        val qualifiedName: Name,
        val table: String,
        val id: IdDefinition? = null,
        val properties: List<PropertyDefinition> = emptyList(),
        val associations: List<AssociationDefinition> = emptyList()
) {
    fun addProperty(column: PropertyDefinition) = this.copy(properties = this.properties + column)

    fun addAssociation(association: AssociationDefinition) = this.copy(associations = this.associations + association)

    fun getPropertyAndIdNames() : List<Name> {
        val props = properties.map(PropertyDefinition::name)
        id?.let { id -> return listOf(id.name) + props } ?: return props
    }

    fun getPropertyNames() = properties.map(PropertyDefinition::name)

    fun getAssociations(vararg types: AssociationType) = associations.filter { it.type in types }

    fun hasManyToMany() = getAssociations(AssociationType.MANY_TO_MANY).isNotEmpty()

    val tableName: String get() = "${name}Table"
    val idColumn: String get() = id?.let { id -> "${tableName}.${id.name}" } ?: throw MissingIdException(this)
}

data class IdDefinition (
        val name: Name,
        val columnName: Name,
        val annotation: Column?,
        val type: IdType,
        val typeMirror: TypeMirror,
        val generatedValue: Boolean = false
)

data class AssociationDefinition(
        val name: Name,
        val target: TypeElement,
        val mapped: Boolean = true,
        val mappedBy: String? = null,
        val joinColumn: String? = null,
        val joinTable: String? = null,
        val type: AssociationType,
        val idType: IdType
)

data class PropertyDefinition(
        val name: Name,
        val columnName: Name,
        val annotation: Column?,
        val type: PropertyType,
        val typeMirror: TypeMirror,
        val nullable: Boolean
)

enum class IdType {
    STRING, LONG, INTEGER, SHORT, UUID
}

enum class PropertyType {
    STRING, BOOL, LONG, DATE, DATETIME, UUID
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
