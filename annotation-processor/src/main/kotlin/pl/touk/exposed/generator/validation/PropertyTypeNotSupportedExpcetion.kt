package pl.touk.exposed.generator.validation

import pl.touk.exposed.generator.model.Type

class PropertyTypeNotSupportedExpcetion(type: Type) :
        RuntimeException("Unsupported conversion to $type")
