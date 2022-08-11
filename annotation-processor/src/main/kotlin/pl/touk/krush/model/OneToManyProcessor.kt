package pl.touk.krush.model

import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.meta.toModelType
import pl.touk.krush.meta.toTypeElement
import javax.persistence.OneToMany

class OneToManyProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
        processElements(annEnv.oneToMany, graphs) { entity, oneToManyElt ->
            val otmAnn = oneToManyElt.getAnnotation(OneToMany::class.java)
            val targetType = oneToManyElt.asType().getTypeArgument().asElement().toTypeElement().toModelType()
            val parentEntityId = graphs.entityId(targetType)
            val mappedBy = otmAnn.mappedBy.takeIf { it.isNotBlank() }
            val associationDef = AssociationDefinition(
                name = oneToManyElt.simpleName.toString(), type = AssociationType.ONE_TO_MANY,
                source = entity.type, target = targetType, mappedBy = mappedBy, targetId = parentEntityId
            )
            entity.addAssociation(associationDef)
        }
}
