package pl.touk.krush.validation

import javax.lang.model.element.TypeElement

class EntityNotMappedException(targetClass: String) :
        RuntimeException("Class $targetClass is missing @Entity annotation") {
                constructor(target: TypeElement) : this("Class ${target.simpleName} is missing @Entity annotation")
        }
