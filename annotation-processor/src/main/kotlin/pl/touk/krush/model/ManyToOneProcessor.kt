package pl.touk.krush.model

import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.meta.isNullable
import pl.touk.krush.meta.joinColumns
import pl.touk.krush.meta.toModelType
import pl.touk.krush.meta.toTypeElement
import pl.touk.krush.meta.toVariableElement

class ManyToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
        processElements(annEnv.manyToOne, graphs) { entity, manyToOneElt ->
            val target = manyToOneElt.toVariableElement().toTypeElement().toModelType()
            val parentEntityId = graphs.entityId(target)
            val associationDef = AssociationDefinition(
                name = manyToOneElt.simpleName.toString(), type = AssociationType.MANY_TO_ONE,
                source = entity.type, target = target, joinColumns = manyToOneElt.joinColumns(), targetId = parentEntityId,
                nullable = manyToOneElt.isNullable()
            )

            entity.id?.let { id ->
                val (enhancedId, enhancedAssoc) = id.handleSharedKey(associationDef)
                entity.addAssociation(enhancedAssoc).copy(id = enhancedId)
            } ?: entity.addAssociation(associationDef)
        }
}
