package pl.touk.krush.validation

import javax.lang.model.element.VariableElement

class ElementTypeNotFoundException(el: VariableElement) :
        RuntimeException("Could not resolve $el type")
