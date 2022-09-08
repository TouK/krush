package pl.touk.krush.model

import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.env.enclosingTypeElement
import pl.touk.krush.meta.asVariable
import pl.touk.krush.meta.joinColumns
import pl.touk.krush.meta.toModelType
import pl.touk.krush.meta.toTypeElement
import pl.touk.krush.validation.EntityNotMappedException
import javax.lang.model.element.Name

class OneToManyPostProcessor(override val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment) : ElementProcessor {

    override fun process(graphs: EntityGraphs) {
        for (oneToMany in annEnv.oneToMany) {
            val entityType = oneToMany.enclosingTypeElement().toModelType()
            val sourceType = oneToMany.asType().getTypeArgument().asElement().toTypeElement().toModelType()

            val graph = graphs[sourceType.packageName] ?: throw EntityNotMappedException(sourceType)
            graph.computeIfPresent(sourceType) { _, entity ->
                val isMapped = entity.getAssociations(AssociationType.MANY_TO_ONE, AssociationType.ONE_TO_ONE)
                    .any { it.target == entityType }
                if (isMapped) {
                    entity
                } else {
                    val parentEntityId = graphs.entityId(entityType)
                    val joinColumns = oneToMany.joinColumns().map(JoinColumnDefinition::from)
                    val associationDef = AssociationDefinition(
                        name = entityType.simpleName.decapitalize(), type = AssociationType.MANY_TO_ONE,
                        source = sourceType, target = entityType, joinColumns = joinColumns, mapped = false, targetId = parentEntityId
                    )
                    entity.addAssociation(associationDef)
                }
            }
        }
    }
}
