package pl.touk.exposed.generator.validation

import javax.lang.model.element.TypeElement

class ConverterTypeNotFoundException(converterEl: TypeElement) :
        RuntimeException("Could not resolve $converterEl converter type")
