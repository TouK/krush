package pl.touk.exposed.generator.validation

import pl.touk.exposed.generator.model.ValidationErrorMessage

class EntityGraphValidationFailedException(errors: List<ValidationErrorMessage>) :
        RuntimeException("Entity Graph validation failed. Errors: ${errors.map { ValidationErrorMessage::text }}")
