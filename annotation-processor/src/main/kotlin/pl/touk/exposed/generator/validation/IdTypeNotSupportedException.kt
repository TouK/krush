package pl.touk.exposed.generator.validation

import pl.touk.exposed.generator.model.Type

class IdTypeNotSupportedException(type: Type) :
        RuntimeException("Unsupported id type $type")