package pl.touk.krush.ksp

import com.google.devtools.ksp.processing.Resolver
import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.meta.isNullable
import pl.touk.krush.meta.joinColumns
import pl.touk.krush.meta.toModelType
import pl.touk.krush.meta.toTypeElement
import pl.touk.krush.meta.toVariableElement
import pl.touk.krush.model.AssociationDefinition
import pl.touk.krush.model.AssociationType
import pl.touk.krush.model.EntityGraphs
import pl.touk.krush.model.JoinColumnDefinition
import pl.touk.krush.model.entityId
import pl.touk.krush.model.handleSharedKey

class ManyToOneProcessor(override val resolver: Resolver, private val annEnv: pl.touk.krush.ksp.AnnotationEnvironment) : DeclarationProcessor {

    override fun process(graphs: EntityGraphs) =
        processElements(annEnv.manyToOne, graphs) { entity, (manyToOneDecl, _) ->
            val target = manyToOneDecl.toModelType()
            val parentEntityId = graphs.entityId(target)
            val joinColumns = manyToOneDecl.joinColumns().map(JoinColumnDefinition::from)
            val associationDef = AssociationDefinition(
                name = manyToOneDecl.simpleName.asString(), type = AssociationType.MANY_TO_ONE,
                source = entity.type, target = target, joinColumns = joinColumns, targetId = parentEntityId,
                nullable = manyToOneDecl.isNullable()
            )

            entity.id?.let { id ->
                val (enhancedId, enhancedAssoc) = id.handleSharedKey(associationDef)
                entity.addAssociation(enhancedAssoc).copy(id = enhancedId)
            } ?: entity.addAssociation(associationDef)
        }
}
