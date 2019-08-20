package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.GeneratedValueWithoutId
import pl.touk.exposed.generator.env.Environment
import pl.touk.exposed.generator.env.enclosingTypeElement
import javax.persistence.Table

class EntityGraphBuilder {

    fun build(env: Environment): EntityGraph {
        val graph = EntityGraph()

        for (entityElt in env.entities) {
            val tableAnn = entityElt.getAnnotation(Table::class.java)
            graph[entityElt] = EntityDefinition(name = entityElt.simpleName, table = tableAnn.name)
        }

        for (idElt in env.ids) {
            val entityType = idElt.enclosingTypeElement()
            graph.computeIfPresent(entityType) { _, entity ->
                entity.copy(id = IdDefinition(idElt.simpleName))
            }
        }

        for (genValueElt in env.genValues) {
            val entityType = genValueElt.enclosingTypeElement()
            graph.computeIfPresent(entityType) { _, entity ->
                val idDefinition = entity.id ?: throw GeneratedValueWithoutId(genValueElt, entityType)
                entity.copy(id = idDefinition.copy(generatedValue = true))
            }
        }

        return graph
    }

}

