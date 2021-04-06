package pl.touk.krush.model

import joinColumns
import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.env.toTypeElement
import pl.touk.krush.env.toVariableElement
import pl.touk.krush.meta.isNullable
import javax.persistence.OneToOne

class OneToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
        processElements(annEnv.oneToOne, graphs) { entity, oneToOneElt ->
            val target = oneToOneElt.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
            val parentEntityId = graphs.entityId(target)
            val mappedBy: String? = oneToOneElt.getAnnotation(OneToOne::class.java)?.mappedBy?.ifBlank { null }

            var associationDef = AssociationDefinition(
                name = oneToOneElt.simpleName, type = AssociationType.ONE_TO_ONE, mapped = mappedBy.isNullOrEmpty(),
                mappedBy = mappedBy, target = target, joinColumns = oneToOneElt.joinColumns(), targetId = parentEntityId,
                nullable = oneToOneElt.isNullable()
            )

            // handle shared column in key
            val sharedId = entity.id?.let { id ->
                var sharedAssoc: AssociationDefinition? = null
                val enhancedProps = mutableListOf<PropertyDefinition>()
                id.properties.forEach { prop ->
                    val sharedColumn = associationDef.joinColumns.find { joinColumn -> joinColumn.name == prop.column?.name }
                    sharedColumn?.let { sharedAssoc = associationDef }
                    enhancedProps.add(prop.copy(sharedColumn = sharedColumn))
                }
                val enhancedId = id.copy(properties = enhancedProps, sharedAssoc = sharedAssoc)
                sharedAssoc?.let { assoc ->
                    associationDef = assoc.copy(sharedId = id)
                }
                enhancedId
            }

            entity.addAssociation(associationDef).copy(id = sharedId)
        }
}
