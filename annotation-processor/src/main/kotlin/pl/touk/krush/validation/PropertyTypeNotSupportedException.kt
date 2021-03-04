package pl.touk.krush.validation

import pl.touk.krush.model.Type

class PropertyTypeNotSupportedException(type: Type) :
        RuntimeException("Unsupported conversion to $type")
