package pl.touk.krush.validation

import pl.touk.krush.model.Type

class AssociationTargetEntityNotFoundException(targetType: Type) :
        RuntimeException("Could not resolve $targetType entity")
