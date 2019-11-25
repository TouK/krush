package pl.touk.krush.validation

import pl.touk.krush.model.Type

class TypeConverterNotSupportedException(type: Type) :
        RuntimeException("Unsupported property type $type")
