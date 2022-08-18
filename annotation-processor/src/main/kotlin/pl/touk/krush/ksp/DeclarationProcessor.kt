package pl.touk.krush.ksp

import com.google.devtools.ksp.processing.Resolver
import pl.touk.krush.meta.toModelType
import pl.touk.krush.model.EntityDefinition
import pl.touk.krush.model.EntityGraphs
import pl.touk.krush.validation.EntityNotMappedException

interface DeclarationProcessor {

    val resolver: Resolver

    fun process(graphs: EntityGraphs)

    fun processElements(elements: List<KSPropertyWithClassDeclaration>, graphs: EntityGraphs,
                        processor: (EntityDefinition, KSPropertyWithClassDeclaration) -> EntityDefinition) {
        for (element in elements) {
            val entityType = element.second.toModelType()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            graph.computeIfPresent(entityType) { _, entity ->
                processor(entity, element)
            }
        }
    }

}
