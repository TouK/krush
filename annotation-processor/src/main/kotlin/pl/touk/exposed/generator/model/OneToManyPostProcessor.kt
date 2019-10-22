package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.enclosingTypeElement
import pl.touk.exposed.generator.env.toTypeElement
import pl.touk.exposed.generator.validation.EntityNotMappedException
import javax.persistence.JoinColumn

class OneToManyPostProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) {
        for (oneToMany in annEnv.oneToMany) {
            val entityType = oneToMany.enclosingTypeElement()
            val joinColumnAnn = oneToMany.getAnnotation(JoinColumn::class.java) ?: continue
            val targetType = oneToMany.asType().getTypeArgument().asElement().toTypeElement()

            val graph = graphs[targetType.packageName] ?: throw EntityNotMappedException(targetType)
            graph.computeIfPresent(targetType) { _, entity ->
                val isMapped = entity.getAssociations(AssociationType.MANY_TO_ONE).any { it.target == entityType }
                if (isMapped) {
                    entity
                } else {
                    val parentEntityId = graphs.entityId(entityType)
                    val associationDef = AssociationDefinition(
                            name = entityType.simpleName, type = AssociationType.MANY_TO_ONE,
                            target = entityType, joinColumn = joinColumnAnn.name, mapped = false,
                            targetId = parentEntityId
                    )
                    entity.addAssociation(associationDef)
                }
            }
        }
    }
}
