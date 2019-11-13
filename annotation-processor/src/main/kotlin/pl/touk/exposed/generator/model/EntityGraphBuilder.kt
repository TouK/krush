package pl.touk.exposed.generator.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import pl.touk.exposed.generator.env.AnnotationEnvironment
import pl.touk.exposed.generator.env.TypeEnvironment
import pl.touk.exposed.generator.model.ValidationResult.Error
import pl.touk.exposed.generator.model.ValidationResult.Success
import pl.touk.exposed.generator.validation.EntityGraphValidationFailedException
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
        when (val annEnvValidation = annEnv.validate()) {
            is Error ->  throw EntityGraphValidationFailedException(annEnvValidation.errors)
        }

        val graphs = EntityGraphs()
        buildEntities(annEnv.entities, graphs)

        processors.forEach { it.process(graphs) }

        when (val entityGraphsValidation = graphs.validate()) {
            is Error ->  throw EntityGraphValidationFailedException(entityGraphsValidation.errors)
        }

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

    private fun AnnotationEnvironment.validate(): ValidationResult {
        return entities.doValidate { entityElt ->
            entityTypeValidators.map { validator -> validator.validate(entityElt) }
        }
    }

    private fun EntityGraphs.validate(): ValidationResult {
        return entities().doValidate { entityDefinition ->
            entityDefValidators.map { validator -> validator.validate(entityDefinition) }
        }
    }

    private fun <T> Iterable<T>.doValidate(validationExpr: (T) -> List<ValidationResult>): ValidationResult {
        val errors = mutableListOf<ValidationErrorMessage>()

        this.forEach { el ->
            val elValidationResults = validationExpr(el)
            elValidationResults.filterIsInstance<Error>().forEach { errors.addAll(it.errors) }
        }

        return if (errors.isEmpty()) Success else Error(errors)
    }
}
