package pl.touk.krush.validation

import javax.lang.model.element.TypeElement

class AssociationTargetEntityNotFoundException(targetEntity: TypeElement) :
        RuntimeException("Could not resolve $targetEntity entity")
