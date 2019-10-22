package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.toTypeElement
import javax.persistence.OneToMany

class OneToManyProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
                processElements(annEnv.oneToMany, graphs) { entity, oneToManyElt ->
                    val otmAnn = oneToManyElt.getAnnotation(OneToMany::class.java)
                    val target = oneToManyElt.asType().getTypeArgument().asElement().toTypeElement()
                    val parentEntityId = graphs.entityId(target)
                    val associationDef = AssociationDefinition(
                            name = oneToManyElt.simpleName, type = AssociationType.ONE_TO_MANY,
                            target = target, mappedBy = otmAnn.mappedBy, targetId = parentEntityId
                    )
                    entity.addAssociation(associationDef)
                }

}
