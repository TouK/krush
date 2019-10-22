package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.toTypeElement
import pl.touk.exposed.generator.env.toVariableElement
import javax.persistence.JoinColumn

class ManyToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
            processElements(annEnv.manyToOne, graphs) { entity, manyToOneElt ->
                val join = manyToOneElt.getAnnotation(JoinColumn::class.java)
                val target = manyToOneElt.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
                val parentEntityId = graphs.entityId(target)
                val associationDef = AssociationDefinition(
                        name = manyToOneElt.simpleName, type = AssociationType.MANY_TO_ONE,
                        target = target, joinColumn = join.name, targetId = parentEntityId
                )
                entity.addAssociation(associationDef)
            }
}
