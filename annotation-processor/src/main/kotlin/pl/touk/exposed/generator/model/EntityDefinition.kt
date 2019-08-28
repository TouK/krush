package pl.touk.exposed.generator.model

import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.persistence.Column

data class EntityDefinition(
        val name: Name,
        val qualifiedName: Name,
        val table: String,
        val id: IdDefinition? = null,
        val properties: List<PropertyDefinition> = emptyList()
) {
    fun addProperty(column: PropertyDefinition) = this.copy(properties = this.properties + column)

    fun getPropertyAndIdNames() : List<Name> {
        val props = properties.map(PropertyDefinition::name)
        id?.let { id -> return listOf(id.name) + props } ?: return props
    }

    fun getPropertyNames() = properties.map(PropertyDefinition::name)
}

data class IdDefinition(
        val name: Name,
        val generatedValue: Boolean = false
)

data class PropertyDefinition(
        val name: Name,
        val annotation: Column,
        val type: TypeDefinition
)

enum class TypeDefinition {
    STRING, BOOL, LONG, DATE, DATETIME
}

typealias EntityGraph = MutableMap<TypeElement, EntityDefinition>

fun EntityGraph(): EntityGraph = mutableMapOf()

fun EntityGraph.traverse(function: (EntityDefinition) -> Unit) {
    this.entries.forEach { (_, value) -> function.invoke(value) }
}

fun Name.asArgument() = this.toString().decapitalize()
