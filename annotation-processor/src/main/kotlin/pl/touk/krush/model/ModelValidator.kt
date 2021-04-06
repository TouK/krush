package pl.touk.krush.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import pl.touk.krush.model.ValidationResult.Error
import pl.touk.krush.model.ValidationResult.Success
import javax.lang.model.element.TypeElement

sealed class ValidationResult {
    object Success : ValidationResult()

    class Error(val errors: List<ValidationErrorMessage>) : ValidationResult() {
        constructor(vararg errors: ValidationErrorMessage) : this(errors.asList())
    }
}

data class ValidationErrorMessage(
        val text: String
)

interface Validator<T> {

    fun validate(el: T): ValidationResult
}

val entityTypeValidators = listOf(
        DataClassValidator()
)

val entityDefValidators = listOf(
        EntityIdValidator(),
        EntityIdTypeValidator(),
        EntityPropertyTypeValidator()
)

class DataClassValidator : Validator<TypeElement> {

    @KotlinPoetMetadataPreview
    override fun validate(el: TypeElement): ValidationResult {
        if (!el.toImmutableKmClass().isData) {
            return Error(ValidationErrorMessage("Entity ${el.qualifiedName} is not data class"))
        }

        return Success
    }

}

class EntityIdValidator : Validator<EntityDefinition> {

    override fun validate(el: EntityDefinition): ValidationResult {
        if (el.id == null) {
            return Error(ValidationErrorMessage("No id field specified for entity ${el.qualifiedName}"))
        } else if (el.id.nullable && !el.id.generatedValue) {
            return Error(ValidationErrorMessage("Nullable id field without @GeneratedValue specified for entity ${el.qualifiedName}"))
        }

        return Success
    }

}

class EntityIdTypeValidator : Validator<EntityDefinition> {

    private val supportedIdTypes = listOf(
        Type("kotlin", "String"),
        Type("kotlin", "Long"),
        Type("kotlin", "Int"),
        Type("kotlin", "Short"),
        Type("java.util", "UUID")
    )

    override fun validate(el: EntityDefinition): ValidationResult {
        el.id!!.properties.forEach { prop ->
            if (prop.converter == null && !prop.isEnumerated() && prop.type !in supportedIdTypes) {
                return Error(ValidationErrorMessage("Entity ${el.qualifiedName} id type ${prop.type} is unsupported. Use property converter instead."))
            }
        }
        return Success
    }
}

class EntityPropertyTypeValidator : Validator<EntityDefinition> {

    private val supportedPropertyTypes = listOf(
        Type("kotlin", "String"),
        Type("kotlin", "Long"),
        Type("kotlin", "Boolean"),
        Type("java.util", "UUID"),
        Type("kotlin", "Int"),
        Type("kotlin", "Short"),
        Type("kotlin", "Float"),
        Type("kotlin", "Double"),
        Type("java.math", "BigDecimal"),
        Type("java.time", "LocalDate"),
        Type("java.time", "LocalDateTime"),
        Type("java.time", "Instant"),
        Type("java.time", "ZonedDateTime")
    )

    override fun validate(el: EntityDefinition): ValidationResult {
        val errors = mutableListOf<ValidationErrorMessage>()
        el.properties
            .filter { !it.hasConverter() && !it.isEnumerated() && it.type !in supportedPropertyTypes && it.type.aliasOf !in supportedPropertyTypes }
            .forEach {
                errors.add(ValidationErrorMessage("Entity ${el.qualifiedName} has unsupported property type ${it.type}"))
        }

        return if (errors.isEmpty()) Success else Error(errors)
    }

}
