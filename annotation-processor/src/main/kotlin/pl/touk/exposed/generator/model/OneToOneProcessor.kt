package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.toTypeElement
import pl.touk.exposed.generator.env.toVariableElement
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

class OneToOneProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) =
            processElements(annEnv.oneToOne, graphs) { entity, oneToOneElt ->
                val join: JoinColumn? = oneToOneElt.getAnnotation(JoinColumn::class.java)
                val target = oneToOneElt.toVariableElement().asType().asDeclaredType().asElement().toTypeElement()
                val parentEntityId = graphs.entityId(target)
                val mappedBy: String? = oneToOneElt.getAnnotation(OneToOne::class.java)?.mappedBy?.ifBlank { null }

                val associationDef = AssociationDefinition(
                        name = oneToOneElt.simpleName, type = AssociationType.ONE_TO_ONE, mapped = mappedBy.isNullOrEmpty(),
                        mappedBy = mappedBy, target = target, joinColumn = join?.name, targetId = parentEntityId
                )
                entity.addAssociation(associationDef)
            }
}
