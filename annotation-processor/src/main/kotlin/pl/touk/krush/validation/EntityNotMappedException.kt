package pl.touk.krush.validation

import pl.touk.krush.model.Type

class EntityNotMappedException(targetClass: String) :
        RuntimeException("Class $targetClass is missing @Entity annotation") {
                constructor(target: Type) : this("Class ${target.simpleName} is missing @Entity annotation")
        }
