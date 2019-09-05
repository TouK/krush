package pl.touk.exposed.generator.validation

import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class GeneratedValueWithoutIdException(varElement: VariableElement, entityType: TypeElement)
    : RuntimeException("@GeneratedValue without @Id on field ${varElement.simpleName} in class ${entityType.simpleName}")
