package pl.touk.krush.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import pl.touk.krush.env.AnnotationEnvironment
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.env.toTypeElement
import pl.touk.krush.env.toVariableElement
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@KotlinPoetMetadataPreview
interface EntityGraphSampleData {

    fun customerTestEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.Customer", typeEnvironment.elementUtils)
    }

    fun defaultPropertyNameEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.DefaultPropertyNameEntity", typeEnvironment.elementUtils)
    }

    fun customPropertyNameEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.CustomPropertyNameEntity", typeEnvironment.elementUtils)
    }

    fun oneToOneTargetEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.OneToOneTargetEntity", typeEnvironment.elementUtils)
    }

    fun oneToOneSourceEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.OneToOneSourceEntity", typeEnvironment.elementUtils)
    }

    fun nullablePropertyEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.NullablePropertyEntity", typeEnvironment.elementUtils)
    }

    fun numericPropertyEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.NumericPropertyEntity", typeEnvironment.elementUtils)
    }

    fun datePropertyEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.DatePropertyEntity", typeEnvironment.elementUtils)
    }

    fun embeddedPropertyEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.EmbeddedPropertyEntity", typeEnvironment.elementUtils)
    }

    fun embeddableType(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.EmbeddableType", typeEnvironment.elementUtils)
    }

    fun enumPropertyEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.EnumPropertyEntity", typeEnvironment.elementUtils)
    }

    fun invalidClassTypeEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.InvalidClassEntity", typeEnvironment.elementUtils)
    }

    fun idNotPresentEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.IdNotPresentEntity", typeEnvironment.elementUtils)
    }

    fun idTypeUnsupportedEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.IdTypeUnsupportedEntity", typeEnvironment.elementUtils)
    }

    fun propertyTypeUnsupportedEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.PropertyTypeUnsupportedEntity", typeEnvironment.elementUtils)
    }

    fun typealiasEntity(typeEnvironment: TypeEnvironment): TypeElement {
        return getTypeElement("pl.touk.example.TypeAliasEntity", typeEnvironment.elementUtils)
    }

    fun customerGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val entity = customerTestEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils, "id")

        val annEnv = AnnotationEnvironment(entities = listOf(entity), ids = listOf(id), columns = emptyList(),
            oneToMany = emptyList(), oneToOne = emptyList(), manyToOne = emptyList(), manyToMany = emptyList(),
            embedded = emptyList(), embeddedColumn = emptyList())

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun customerTestEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val entity = customerTestEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils, "id")

        return EntityDefinition(
            type = entity, table = "customers",
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)))
    }

    fun validTableMappingGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val defaultPropertyNameEntity = defaultPropertyNameEntity(typeEnvironment)
        val defaultPropertyNameEntityId = getVariableElement(defaultPropertyNameEntity, elements, "id")
        val defaultPropertyNameEntityProp1 = getVariableElement(defaultPropertyNameEntity, elements, "prop1")
        val defaultPropertyNameEntityProp2 = getVariableElement(defaultPropertyNameEntity, elements,"prop2")

        val customPropertyNameEntity = customPropertyNameEntity(typeEnvironment)
        val customPropertyNameEntityId = getVariableElement(customPropertyNameEntity, elements, "id")
        val customPropertyNameEntityProp1 = getVariableElement(customPropertyNameEntity, elements, "prop1")

        val annEnv = AnnotationEnvironment(
            entities = listOf(defaultPropertyNameEntity, customPropertyNameEntity),
            ids = listOf(customPropertyNameEntityId, defaultPropertyNameEntityId),
            columns = listOf(defaultPropertyNameEntityProp1, defaultPropertyNameEntityProp2, customPropertyNameEntityProp1),
            oneToMany = emptyList(), oneToOne = emptyList(), manyToOne = emptyList(), manyToMany = emptyList(),
            embedded = emptyList(), embeddedColumn = emptyList()
        )
        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun defaultPropertyNameEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = defaultPropertyNameEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val prop1 = getVariableElement(entity, elements,"prop1")
        val prop2 = getVariableElement(entity, elements,"prop2")

        return EntityDefinition(
            type = entity, table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            properties = listOf(
                propertyDefinition(typeEnvironment, prop1, "prop1", STRING, false),
                propertyDefinition(typeEnvironment, prop2, "prop2", STRING, false)
            )
        )
    }

    fun customPropertyNameEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = customPropertyNameEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val prop1 = getVariableElement(entity, elements,"prop1")

        return EntityDefinition(
            type = entity,
            table = "entity",
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.getAnnotation(Column::class.java).name)),
            properties = listOf(
                propertyDefinition(typeEnvironment, prop1, "prop1_custom", STRING, false)
            )
        )
    }

    fun nullablePropertyGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val entity = nullablePropertyEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils,"id")
        val prop1 = getVariableElement(entity, typeEnvironment.elementUtils,"prop1")

        val annEnv = AnnotationEnvironment(
            entities = listOf(entity),
            ids = listOf(id), columns = listOf(prop1),
            oneToMany = emptyList(), oneToOne = emptyList(), manyToMany = emptyList(), manyToOne = emptyList(),
            embedded = emptyList(), embeddedColumn = emptyList()
        )

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun nullablePropertyEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val entity = nullablePropertyEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils,"id")
        val prop1 = getVariableElement(entity, typeEnvironment.elementUtils,"prop1")

        return EntityDefinition(
            type = entity,
            table = "nullablePropertyEntity",
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            properties = listOf(
                propertyDefinition(typeEnvironment, prop1,"prop1", STRING, true)
            )
        )
    }

    fun oneToOneGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val sourceEntity = oneToOneSourceEntity(typeEnvironment)
        val sourceEntityId = getVariableElement(sourceEntity, elements, "id")
        val sourceEntityTarget = getVariableElement(sourceEntity, elements, "targetEntity")

        val targetEntity = oneToOneTargetEntity(typeEnvironment)
        val targetEntityId = getVariableElement(targetEntity, elements, "id")
        val targetEntitySource = getVariableElement(targetEntity, elements, "sourceEntity")

        val annEnv = AnnotationEnvironment(
            entities = listOf(targetEntity, sourceEntity),
            ids = listOf(sourceEntityId, targetEntityId),
            columns = emptyList(),
            oneToOne = listOf(sourceEntityTarget, targetEntitySource),
            oneToMany = emptyList(), manyToOne = emptyList(), manyToMany = emptyList(), embedded = emptyList(),
            embeddedColumn = emptyList()
        )

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun oneToOneSourceEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = oneToOneSourceEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val targetEntity = getVariableElement(entity, elements,"targetEntity")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            associations = listOf(
                AssociationDefinition(
                    name =  targetEntity.simpleName,
                    source = entity,
                    target = targetEntity.toVariableElement().asType().asDeclaredType().asElement().toTypeElement(),
                    joinColumns = listOf(targetEntity.getAnnotation(JoinColumn::class.java)),
                    type = AssociationType.ONE_TO_ONE,
                    targetId = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
                    mapped = true
                )
            )
        )
    }

    fun oneToOneTargetEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = oneToOneTargetEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val sourceEntity = getVariableElement(entity, elements, "sourceEntity")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            associations = listOf(
                AssociationDefinition(
                    name = sourceEntity.simpleName,
                    source = entity,
                    target = sourceEntity.toVariableElement().asType().asDeclaredType().asElement().toTypeElement(),
                    type = AssociationType.ONE_TO_ONE,
                    targetId = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
                    mapped = false,
                    mappedBy = sourceEntity.getAnnotation(OneToOne::class.java).mappedBy
                )
            )
        )
    }

    fun numericPropertyGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val numericPropertyEntity = numericPropertyEntity(typeEnvironment)
        val numericPropertyEntityId = getVariableElement(numericPropertyEntity, elements, "id")
        val long = getVariableElement(numericPropertyEntity, typeEnvironment.elementUtils,"long")
        val int = getVariableElement(numericPropertyEntity, typeEnvironment.elementUtils,"int")
        val short = getVariableElement(numericPropertyEntity, typeEnvironment.elementUtils,"short")
        val float = getVariableElement(numericPropertyEntity, typeEnvironment.elementUtils,"float")
        val double = getVariableElement(numericPropertyEntity, typeEnvironment.elementUtils,"double")

        val annEnv = AnnotationEnvironment(entities =  listOf(numericPropertyEntity), ids = listOf(numericPropertyEntityId),
            columns = listOf(long, int, short, float, double), oneToMany = emptyList(), manyToOne = emptyList(),
            manyToMany =  emptyList(), oneToOne = emptyList(), embedded = emptyList(), embeddedColumn = emptyList())

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun numericPropertyEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = numericPropertyEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val long = getVariableElement(entity, typeEnvironment.elementUtils,"long")
        val int = getVariableElement(entity, typeEnvironment.elementUtils,"int")
        val short = getVariableElement(entity, typeEnvironment.elementUtils,"short")
        val float = getVariableElement(entity, typeEnvironment.elementUtils,"float")
        val double = getVariableElement(entity, typeEnvironment.elementUtils,"double")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            properties = listOf(
                propertyDefinition(typeEnvironment, long, "long", LONG, false),
                propertyDefinition(typeEnvironment, int, "int", INT, false),
                propertyDefinition(typeEnvironment, short, "short", SHORT, false),
                propertyDefinition(typeEnvironment, float, "float", FLOAT, false),
                propertyDefinition(typeEnvironment, double, "double", DOUBLE, false)
            )
        )
    }

    fun datePropertyGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val datePropertyEntity = datePropertyEntity(typeEnvironment)
        val datePropertyEntityId = getVariableElement(datePropertyEntity, elements, "id")
        val localDate = getVariableElement(datePropertyEntity, typeEnvironment.elementUtils,"localDate")
        val localDateTime = getVariableElement(datePropertyEntity, typeEnvironment.elementUtils,"localDateTime")
        val instant = getVariableElement(datePropertyEntity, typeEnvironment.elementUtils,"instant")
        val zonedDateTime = getVariableElement(datePropertyEntity, typeEnvironment.elementUtils,"zonedDateTime")

        val annEnv = AnnotationEnvironment(entities =  listOf(datePropertyEntity), ids = listOf(datePropertyEntityId),
            columns = listOf(localDate, localDateTime, instant, zonedDateTime), oneToMany = emptyList(), manyToOne = emptyList(),
            manyToMany =  emptyList(), oneToOne = emptyList(), embedded = emptyList(), embeddedColumn = emptyList())

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun datePropertyEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = datePropertyEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val localDate = getVariableElement(entity, typeEnvironment.elementUtils,"localDate")
        val localDateTime = getVariableElement(entity, typeEnvironment.elementUtils,"localDateTime")
        val instant = getVariableElement(entity, typeEnvironment.elementUtils,"instant")
        val zonedDateTime = getVariableElement(entity, typeEnvironment.elementUtils,"zonedDateTime")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            properties = listOf(
                propertyDefinition(typeEnvironment, localDate, "localDate", LOCAL_DATE, false),
                propertyDefinition(typeEnvironment, localDateTime, "localDateTime", LOCAL_DATE_TIME, false),
                propertyDefinition(typeEnvironment, instant, "instant", INSTANT, false),
                propertyDefinition(typeEnvironment, zonedDateTime, "zonedDateTime", ZONED_DATE_TIME, false)
            )
        )
    }

    fun embeddedPropertyGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val embeddedPropertyEntity = embeddedPropertyEntity(typeEnvironment)
        val embeddedPropertyEntityId = getVariableElement(embeddedPropertyEntity, elements, "id")
        val embeddableType = getVariableElement(embeddedPropertyEntity, typeEnvironment.elementUtils,"embeddableType")
        val property1 = getVariableElement(embeddableType(typeEnvironment), typeEnvironment.elementUtils,"property1")

        val annEnv = AnnotationEnvironment(entities =  listOf(embeddedPropertyEntity), ids = listOf(embeddedPropertyEntityId),
            columns = emptyList(), oneToMany = emptyList(), manyToOne = emptyList(),
            manyToMany =  emptyList(), oneToOne = emptyList(), embedded = listOf(embeddableType), embeddedColumn = listOf(property1))

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun embeddedPropertyEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = embeddedPropertyEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val property1 = getVariableElement(embeddableType(typeEnvironment), typeEnvironment.elementUtils,"property1")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            embeddables = listOf(
                EmbeddableDefinition(
                    propertyName = typeEnvironment.elementUtils.getName("embeddableType"),
                    qualifiedName = typeEnvironment.elementUtils.getName("pl.touk.example.EmbeddableType"),
                    nullable = false,
                    properties = listOf(
                        propertyDefinition(typeEnvironment, property1, "property1", STRING, false)
                    )
                )
            )
        )
    }

    fun enumPropertyGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val enumPropertyEntity = enumPropertyEntity(typeEnvironment)
        val enumPropertyEntityId = getVariableElement(enumPropertyEntity, elements, "id")
        val enumClass = getVariableElement(enumPropertyEntity, typeEnvironment.elementUtils,"enumClass")

        val annEnv = AnnotationEnvironment(entities =  listOf(enumPropertyEntity), ids = listOf(enumPropertyEntityId),
            columns = listOf(enumClass), oneToMany = emptyList(), manyToOne = emptyList(),
            manyToMany =  emptyList(), oneToOne = emptyList(), embedded = emptyList(), embeddedColumn = emptyList())

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun enumPropertyEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val elements = typeEnvironment.elementUtils

        val entity = enumPropertyEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val enumClass = getVariableElement(entity, typeEnvironment.elementUtils,"enumClass")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            properties = listOf(
                propertyDefinition(typeEnvironment, enumClass, "enumClass", Type("pl.touk.example", "EnumClass"), false)
                    .copy(enumerated = EnumeratedDefinition(EnumType.STRING))
            ),
            embeddables = emptyList()
        )
    }

    fun idNotPresentEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val entity = idNotPresentEntity(typeEnvironment)

        return EntityDefinition(
            type = entity, id = null,
            table = entity.simpleName.asVariable(), properties = emptyList(), embeddables = emptyList()
        )
    }

    fun idTypeUnsupportedEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val entity = idTypeUnsupportedEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils, "id")

        val idPropDef = PropertyDefinition(
            name = id.simpleName, columnName = typeEnvironment.elementUtils.getName("id"),
            column = id.getAnnotation(Column::class.java), type = FLOAT, nullable = true
        )

        return EntityDefinition(
            type = entity,
            id = IdDefinition(name = id.simpleName, type = FLOAT, generatedValue = true, properties = listOf(idPropDef), nullable = true),
            table = entity.simpleName.asVariable(), properties = emptyList(), embeddables = emptyList()
        )
    }

    fun propertyTypeUnsupportedEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val entity = propertyTypeUnsupportedEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils, "id")
        val prop = getVariableElement(entity, typeEnvironment.elementUtils,"prop")

        return EntityDefinition(
            type = entity,
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            table = entity.simpleName.asVariable(),
            properties = listOf(
                propertyDefinition(typeEnvironment, prop, "prop", Type("kotlin", "Pair"), false)
            ),
            embeddables = emptyList()
        )
    }

    fun typealiasGraphBuilder(typeEnvironment: TypeEnvironment): EntityGraphBuilder {
        val elements = typeEnvironment.elementUtils

        val entity = typealiasEntity(typeEnvironment)
        val id = getVariableElement(entity, elements, "id")
        val aliased = getVariableElement(entity, elements, "aliased")
        val plainString = getVariableElement(entity, elements, "justAString")

        val annEnv = AnnotationEnvironment(entities = listOf(entity), ids = listOf(id),
            columns = listOf(aliased, plainString), oneToMany = emptyList(), manyToOne = emptyList(),
            manyToMany = emptyList(), oneToOne = emptyList(), embedded = emptyList(), embeddedColumn = emptyList())

        return EntityGraphBuilder(typeEnvironment, annEnv)
    }

    fun typealiasEntityDefinition(typeEnvironment: TypeEnvironment): EntityDefinition {
        val entity = typealiasEntity(typeEnvironment)
        val id = getVariableElement(entity, typeEnvironment.elementUtils, "id")
        val prop1 = getVariableElement(entity, typeEnvironment.elementUtils, "aliased")
        val plainString = getVariableElement(entity, typeEnvironment.elementUtils, "justAString")

        return EntityDefinition(
            type = entity,
            table = entity.simpleName.asVariable(),
            id = autoGenIdDefinition(id, typeEnvironment.elementUtils.getName(id.simpleName)),
            properties = listOf(
                propertyDefinition(
                    typeEnvironment,
                    prop1,
                    "aliased",
                    Type(
                        packageName = "pl.touk.example",
                        simpleName = "StringMap",
                        aliasOf = Type("kotlin.collections","Map")
                    ),
                    false
                ).copy(converter =
                    ConverterDefinition(
                        name = "pl.touk.example.StringMapConverter",
                        targetType = Type(packageName = "kotlin", simpleName = "String"),
                        isObject = false
                    )
                ),
                propertyDefinition(
                    typeEnvironment,
                    plainString,
                    "justAString",
                    Type(
                        packageName = "pl.touk.example",
                        simpleName = "PlainString",
                        aliasOf = Type("kotlin", "String")
                    ),
                    false
                )
            )
        )
    }

    private fun autoGenIdDefinition(id: VariableElement, name: Name): IdDefinition {
        val idPropDef = PropertyDefinition(
            name = id.simpleName,
            columnName = name,
            column = id.getAnnotation(Column::class.java),
            type = LONG,
            nullable = true
        )
        return IdDefinition(
            name = id.simpleName,
            type = LONG,
            generatedValue = true,
            nullable = true,
            properties = listOf(idPropDef)
        )
    }

    private fun propertyDefinition(typeEnvironment: TypeEnvironment, property: VariableElement, columnName: String, type: Type, nullable: Boolean): PropertyDefinition {
        return PropertyDefinition(
            name = typeEnvironment.elementUtils.getName(property.simpleName),
            columnName = typeEnvironment.elementUtils.getName(columnName),
            column = property.getAnnotation(Column::class.java),
            type = type,
            nullable = nullable
        )
    }

    private fun getTypeElement(name: String, elements: Elements): TypeElement = elements.getTypeElement(name)

    private fun getVariableElement(typeElt: TypeElement, elements: Elements, name: String): VariableElement =
        elements.getAllMembers(typeElt)
            .filter { it.simpleName.contentEquals(name) }
            .map(Element::toVariableElement)
            .first()

    private fun TypeMirror.asDeclaredType(): DeclaredType {
        require(this is DeclaredType)
        return this
    }
}

private const val KOTLIN_PKG = "kotlin"
private const val JAVA_TIME_PKG = "java.time"
@JvmField val STRING = Type(KOTLIN_PKG, "String")
@JvmField val LONG = Type(KOTLIN_PKG, "Long")
@JvmField val INT = Type(KOTLIN_PKG, "Int")
@JvmField val SHORT = Type(KOTLIN_PKG, "Short")
@JvmField val FLOAT = Type(KOTLIN_PKG, "Float")
@JvmField val DOUBLE = Type(KOTLIN_PKG, "Double")
@JvmField val LOCAL_DATE = Type(JAVA_TIME_PKG, "LocalDate")
@JvmField val LOCAL_DATE_TIME = Type(JAVA_TIME_PKG, "LocalDateTime")
@JvmField val ZONED_DATE_TIME = Type(JAVA_TIME_PKG, "ZonedDateTime")
@JvmField val INSTANT = Type(JAVA_TIME_PKG, "Instant")
