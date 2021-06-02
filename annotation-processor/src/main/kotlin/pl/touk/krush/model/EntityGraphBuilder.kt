package pl.touk.krush.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.model.ValidationResult.Error
import pl.touk.krush.model.ValidationResult.Success
import pl.touk.krush.validation.EntityGraphValidationFailedException
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

        val graphs = buildEntities(annEnv.entities)

        processors.forEach { it.process(graphs) }

        when (val entityGraphsValidation = graphs.validate()) {
            is Error ->  throw EntityGraphValidationFailedException(entityGraphsValidation.errors)
        }

        return graphs
    }

    private fun buildEntities(entityList: List<TypeElement>) : EntityGraphs {
        val graphs = EntityGraphs()
        for (entityElt in entityList) {
            val graph = graphs.getOrDefault(entityElt.packageName, EntityGraph())
            graph[entityElt] = EntityDefinition(type = entityElt, table = entityElt.tableName)
            graphs[entityElt.packageName] = graph
        }
        return graphs
    }

    private fun AnnotationEnvironment.validate(): ValidationResult {
        return entities.doValidate(entityTypeValidators)
    }

    private fun EntityGraphs.validate(): ValidationResult {
        return entities().doValidate(entityDefValidators)
    }

    private fun <T> Iterable<T>.doValidate(validators: List<Validator<T>>): ValidationResult {
        val errors = mutableListOf<ValidationErrorMessage>()

        this.forEach { el ->
            validators.forEach { validator ->
                try {
                    when (val elValidationResult = validator.validate(el)) {
                        is Error -> errors.addAll(elValidationResult.errors)
                    }
                } catch (ex: Exception) {
                    errors.add(ValidationErrorMessage("Validation exception: $ex, element: $el, validator: $validator"))
                }
            }
        }

        return if (errors.isEmpty()) Success else Error(errors)
    }
}
