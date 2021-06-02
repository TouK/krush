package pl.touk.krush.model

import joinColumns
import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.env.toTypeElement
import pl.touk.krush.env.toVariableElement
import pl.touk.krush.meta.isNullable
import javax.persistence.JoinColumn

class ManyToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
        processElements(annEnv.manyToOne, graphs) { entity, manyToOneElt ->
            val target = manyToOneElt.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
            val parentEntityId = graphs.entityId(target)
            val associationDef = AssociationDefinition(
                name = manyToOneElt.simpleName, type = AssociationType.MANY_TO_ONE,
                source = entity.type, target = target, joinColumns = manyToOneElt.joinColumns(), targetId = parentEntityId,
                nullable = manyToOneElt.isNullable()
            )

            entity.id?.let { id ->
                val (enhancedId, enhancedAssoc) = id.handleSharedKey(associationDef)
                entity.addAssociation(enhancedAssoc).copy(id = enhancedId)
            } ?: entity.addAssociation(associationDef)
        }
}
