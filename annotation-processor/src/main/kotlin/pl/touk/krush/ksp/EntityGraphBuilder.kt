package pl.touk.krush.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import pl.touk.krush.meta.tableName
import pl.touk.krush.meta.toModelType
import pl.touk.krush.model.EntityDefinition
import pl.touk.krush.model.EntityGraph
import pl.touk.krush.model.EntityGraphs
import pl.touk.krush.model.ValidationErrorMessage
import pl.touk.krush.model.ValidationResult
import pl.touk.krush.model.ValidationResult.Error
import pl.touk.krush.model.ValidationResult.Success
import pl.touk.krush.model.Validator
import pl.touk.krush.validation.EntityGraphValidationFailedException

class EntityGraphBuilder(resolver: Resolver, val annEnv: AnnotationEnvironment) {

    private val processors = listOf(
        ColumnProcessor(resolver, annEnv),
//        OneToOneProcessor(typeEnv, annEnv),
//        OneToManyProcessor(typeEnv, annEnv),
        ManyToOneProcessor(resolver, annEnv),
//        ManyToManyProcessor(typeEnv, annEnv),
//        OneToManyPostProcessor(typeEnv, annEnv)
    )

    fun build(): EntityGraphs {
        when (val annEnvValidation = annEnv.validate()) {
            is Error ->  throw EntityGraphValidationFailedException(annEnvValidation.errors)
            else -> {}
        }

        val graphs = buildEntities(annEnv.entities)

        processors.forEach { it.process(graphs) }

        when (val entityGraphsValidation = graphs.validate()) {
            is Error ->  throw EntityGraphValidationFailedException(entityGraphsValidation.errors)
            else -> {}
        }

        return graphs
    }

    private fun buildEntities(entityList: List<KSClassDeclaration>) : EntityGraphs {
        val graphs = EntityGraphs()
        for (entityElt in entityList) {
            val modelType = entityElt.toModelType()
            val graph = graphs.getOrDefault(modelType.packageName, EntityGraph())
            graph[modelType] = EntityDefinition(type = modelType, table = entityElt.tableName)
            graphs[modelType.packageName] = graph
        }
        return graphs
    }

    private fun AnnotationEnvironment.validate(): ValidationResult {
        return Success
//        return entities.doValidate(entityTypeValidators)
    }

    private fun EntityGraphs.validate(): ValidationResult {
        return Success
//        return entities().doValidate(entityDefValidators)
    }

    private fun <T> Iterable<T>.doValidate(validators: List<Validator<T>>): ValidationResult {
        val errors = mutableListOf<ValidationErrorMessage>()

        this.forEach { el ->
            validators.forEach { validator ->
                try {
                    when (val elValidationResult = validator.validate(el)) {
                        is Error -> errors.addAll(elValidationResult.errors)
                        else -> {}
                    }
                } catch (ex: Exception) {
                    errors.add(ValidationErrorMessage("Validation exception: $ex, element: $el, validator: $validator"))
                }
            }
        }

        return if (errors.isEmpty()) Success else Error(errors)
    }
}
