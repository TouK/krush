package pl.touk.krush.model

import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.meta.isNullable
import pl.touk.krush.meta.joinColumns
import pl.touk.krush.meta.toModelType
import pl.touk.krush.meta.toTypeElement
import pl.touk.krush.meta.toVariableElement
import javax.persistence.OneToOne

class OneToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
        processElements(annEnv.oneToOne, graphs) { entity, oneToOneElt ->
            val targetType = oneToOneElt.toVariableElement().toTypeElement().toModelType()
            val parentEntityId = graphs.entityId(targetType)
            val mappedBy: String? = oneToOneElt.getAnnotation(OneToOne::class.java)?.mappedBy?.ifBlank { null }

            val joinColumns = oneToOneElt.joinColumns().map(JoinColumnDefinition::from)
            val associationDef = AssociationDefinition(
                name = oneToOneElt.simpleName.toString(), type = AssociationType.ONE_TO_ONE,
                mapped = mappedBy.isNullOrEmpty(), mappedBy = mappedBy,
                source = entity.type, target = targetType,
                joinColumns = joinColumns, targetId = parentEntityId,
                nullable = oneToOneElt.isNullable()
            )

            entity.id?.let { id ->
                val (enhancedId, enhancedAssoc) = id.handleSharedKey(associationDef)
                entity.addAssociation(enhancedAssoc).copy(id = enhancedId)
            } ?: entity.addAssociation(associationDef)
        }
}
