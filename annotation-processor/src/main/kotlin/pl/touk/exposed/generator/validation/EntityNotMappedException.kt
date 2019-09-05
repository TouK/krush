package pl.touk.exposed.generator.validation

import java.lang.RuntimeException
import javax.lang.model.element.TypeElement

class EntityNotMappedException(target: TypeElement) :
        RuntimeException("Class ${target.simpleName} in missing @Entity annotation")
