package pl.touk.exposed.generator.validation

import pl.touk.exposed.generator.model.PropertyType

class TypeConverterNotSupportedException(type: PropertyType) :
        RuntimeException("Unsupported conversion to $type")
