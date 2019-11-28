package pl.touk.krush.validation

import pl.touk.krush.model.ValidationErrorMessage

class EntityGraphValidationFailedException(errors: List<ValidationErrorMessage>) :
        RuntimeException("Entity Graph validation failed. Errors: ${errors.joinToString(prefix = "\n", separator = "\n")}")
