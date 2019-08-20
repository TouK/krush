package pl.touk.exposed.generator

import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class GeneratedValueWithoutId(varElement: VariableElement, entityType: TypeElement)
    : RuntimeException("@GeneratedValue without @Id on field ${varElement.simpleName} in class ${entityType.simpleName}")
