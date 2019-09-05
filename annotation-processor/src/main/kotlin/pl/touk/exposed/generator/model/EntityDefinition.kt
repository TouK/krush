package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.validation.MissingIdException
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.persistence.Column

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

    val tableName: String get() = "${name}Table"
    val idColumn: String get() = id?.let { id -> "${tableName}.${id.name}" } ?: throw MissingIdException(this)
}

data class IdDefinition(
        val name: Name,
        val generatedValue: Boolean = false
)

data class AssociationDefinition(
        val name: Name,
        val target: TypeElement,
        val mapped: Boolean = true,
        val mappedBy: String? = null,
        val joinColumn: String? = null,
        val type: AssociationType
)

data class PropertyDefinition(
        val name: Name,
        val annotation: Column,
        val type: PropertyType
)

enum class PropertyType {
    STRING, BOOL, LONG, DATE, DATETIME
}

enum class AssociationType {
    ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
}

typealias EntityGraph = MutableMap<TypeElement, EntityDefinition>

fun EntityGraph(): EntityGraph = mutableMapOf()

fun EntityGraph.traverse(function: (EntityDefinition) -> Unit) {
    this.entries.forEach { (_, value) -> function.invoke(value) }
}

fun EntityGraph.traverse(function: (TypeElement, EntityDefinition) -> Unit) {
    this.entries.forEach { (key, value) -> function.invoke(key, value) }
}

fun Name.asVariable() = this.toString().decapitalize()
