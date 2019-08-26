package pl.touk.exposed.generator.model

import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.persistence.Column

data class EntityDefinition(
        val name: Name,
        val qualifiedName: Name,
        val table: String,
        val id: IdDefinition? = null,
        val columns: List<ColumnDefinition> = emptyList()
) {
    fun addColumn(column: ColumnDefinition) = this.copy(columns = this.columns + column)
}

data class IdDefinition(
        val name: Name,
        val generatedValue: Boolean = false
)

data class ColumnDefinition(
        val name: Name,
        val annotation: Column,
        val type: TypeDefinition
)

enum class TypeDefinition {
    STRING, BOOL, LONG, DATE, DATETIME
}

typealias EntityGraph = MutableMap<TypeElement, EntityDefinition>

fun EntityGraph(): EntityGraph = mutableMapOf()
