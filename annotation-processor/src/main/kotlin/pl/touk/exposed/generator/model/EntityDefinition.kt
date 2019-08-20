package pl.touk.exposed.generator.model

import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

data class EntityDefinition(
        val name: Name,
        val table: String,
        val id: IdDefinition? = null
)

data class IdDefinition(
        val name: Name,
        val generatedValue: Boolean = false
)

data class ColumnDefinition(
        val name: Name,
        val propertyName: String,
        val typeMirror: TypeMirror,
        val type: TypeDefinition
)

enum class TypeDefinition {
    NUMBER, STRING, DATE
}

typealias EntityGraph = MutableMap<TypeElement, EntityDefinition>

fun EntityGraph(): EntityGraph = mutableMapOf()
