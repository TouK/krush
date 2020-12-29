package pl.touk.krush.model

import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.env.toTypeElement
import pl.touk.krush.env.toVariableElement
import pl.touk.krush.meta.isNullable
import javax.persistence.JoinColumn

class ManyToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
            processElements(annEnv.manyToOne, graphs) { entity, manyToOneElt ->
                val join = manyToOneElt.getAnnotation(JoinColumn::class.java)
                val target = manyToOneElt.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
                val parentEntityId = graphs.entityId(target)
                val associationDef = AssociationDefinition(
                    name = manyToOneElt.simpleName, type = AssociationType.MANY_TO_ONE,
                    target = target, joinColumn = join.name, targetId = parentEntityId,
                    nullable = manyToOneElt.isNullable()
                )
                entity.addAssociation(associationDef)
            }
}
