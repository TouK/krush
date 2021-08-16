package pl.touk.krush.model

import java.util.*

fun IdDefinition.handleSharedKey(assoc: AssociationDefinition): Pair<IdDefinition, AssociationDefinition> {
    var sharedAssoc: AssociationDefinition? = null
    val enhancedProps = mutableListOf<PropertyDefinition>()
    var enhancedAssoc = assoc
    properties.forEach { prop ->
        val sharedColumn = assoc.joinColumns.find { joinColumn -> joinColumn.name == prop.column?.name }
        sharedColumn?.let { sharedAssoc = assoc }
        enhancedProps.add(prop.copy(sharedColumn = sharedColumn))
    }
    val enhancedId = copy(properties = enhancedProps, sharedAssoc = sharedAssoc)
    sharedAssoc?.let { enhancedAssoc = it.copy(sharedId = this) }

    return enhancedId to enhancedAssoc
}

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
fun String.decapitalize() = this.replaceFirstChar { it.lowercase(Locale.getDefault()) }
fun String.toLowerCase() = this.lowercase(Locale.getDefault())
