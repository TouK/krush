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
            val targetType = oneToOneElt.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
            val parentEntityId = graphs.entityId(targetType)
            val mappedBy: String? = oneToOneElt.getAnnotation(OneToOne::class.java)?.mappedBy?.ifBlank { null }

            val associationDef = AssociationDefinition(
                name = oneToOneElt.simpleName, type = AssociationType.ONE_TO_ONE,
                mapped = mappedBy.isNullOrEmpty(), mappedBy = mappedBy,
                source = entity.type, target = targetType,
                joinColumns = oneToOneElt.joinColumns(), targetId = parentEntityId,
                nullable = oneToOneElt.isNullable()
            )

            entity.id?.let { id ->
                val (enhancedId, enhancedAssoc) = id.handleSharedKey(associationDef)
                entity.addAssociation(enhancedAssoc).copy(id = enhancedId)
            } ?: entity.addAssociation(associationDef)
        }
}
