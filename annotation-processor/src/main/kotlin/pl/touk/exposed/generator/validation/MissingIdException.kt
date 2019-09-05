package pl.touk.exposed.generator.validation

import pl.touk.exposed.generator.model.EntityDefinition
import java.lang.RuntimeException

class MissingIdException(entityDefinition: EntityDefinition) :
        RuntimeException("Entity ${entityDefinition.name} is missing property with @Id annotation")
