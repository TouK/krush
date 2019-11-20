package pl.touk.krush.validation

import pl.touk.krush.model.Type

class IdTypeNotSupportedException(type: Type) :
        RuntimeException("Unsupported id type $type")
