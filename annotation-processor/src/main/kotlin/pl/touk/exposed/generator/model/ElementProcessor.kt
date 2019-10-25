package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.env.enclosingTypeElement
import pl.touk.exposed.generator.validation.EntityNotMappedException
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

interface ElementProcessor {

    val typeEnv: TypeEnvironment

    fun process(graphs: EntityGraphs)

    fun TypeMirror.asDeclaredType(): DeclaredType {
        require(this is DeclaredType)
        return this
    }

    fun TypeMirror.getTypeArgument(): DeclaredType {
        return this.asDeclaredType().typeArguments[0].asDeclaredType()
    }

    fun processElements(elements: List<VariableElement>, graphs: EntityGraphs,
                                 processor: (EntityDefinition, VariableElement) -> EntityDefinition) {
        for (element in elements) {
            val entityType = element.enclosingTypeElement()
            val graph = graphs[entityType.packageName] ?: throw EntityNotMappedException(entityType)
            graph.computeIfPresent(entityType) { _, entity ->
                processor(entity, element)
            }
        }
    }

}
