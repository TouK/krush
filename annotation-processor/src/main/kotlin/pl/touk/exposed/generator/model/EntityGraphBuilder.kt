package pl.touk.exposed.generator.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
class EntityGraphBuilder(
        typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment
) {

    private val processors = listOf(
            ColumnProcessor(typeEnv, annEnv),
            OneToOneProcessor(typeEnv, annEnv),
            OneToManyProcessor(typeEnv, annEnv),
            ManyToOneProcessor(typeEnv, annEnv),
            ManyToManyProcessor(typeEnv, annEnv),
            OneToManyPostProcessor(typeEnv, annEnv)
    )

    fun build(): EntityGraphs {
        val graphs = EntityGraphs()

        buildEntities(annEnv.entities, graphs)

        processors.forEach { it.process(graphs) }

        return graphs
    }

    private fun buildEntities(entityList: List<TypeElement>, graphs: EntityGraphs) {
        for (entityElt in entityList) {
            val graph = graphs.getOrDefault(entityElt.packageName, EntityGraph())
            graph[entityElt] = EntityDefinition(
                    name = entityElt.simpleName, qualifiedName = entityElt.qualifiedName, table = entityElt.tableName
            )
            graphs[entityElt.packageName] = graph
        }
    }

}
