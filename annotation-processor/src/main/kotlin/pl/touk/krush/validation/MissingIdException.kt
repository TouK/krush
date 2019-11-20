package pl.touk.krush.validation

import pl.touk.krush.model.EntityDefinition
import java.lang.RuntimeException

class MissingIdException(entityDefinition: EntityDefinition) :
        RuntimeException("Entity ${entityDefinition.name} is missing property with @Id annotation")
