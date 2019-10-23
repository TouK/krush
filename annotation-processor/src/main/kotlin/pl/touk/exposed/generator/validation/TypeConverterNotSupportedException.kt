package pl.touk.exposed.generator.validation

import pl.touk.exposed.generator.model.Type

class TypeConverterNotSupportedException(type: Type) :
        RuntimeException("Unsupported property type $type")
