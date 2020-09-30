package pl.touk.krush

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.JavaInstantColumnType
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.math.BigDecimal
import java.time.Instant
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class WrapperColumnType<Raw : Any, out Wrapper : Any>(
    protected val rawColumnType: ColumnType,
    internal val rawClazz: KClass<Raw>,
    private val wrapperClazz: KClass<Wrapper>,
    protected val instanceCreator: (Raw) -> Wrapper,
    private val valueExtractor: (Wrapper) -> Raw
) : ColumnType() {

    override fun sqlType(): String = rawColumnType.sqlType()

    override fun valueFromDB(value: Any) = rawColumnType.valueFromDB(value).let { rawValue ->
        when {
            rawClazz.isInstance(rawValue) -> instanceCreator(rawValue as Raw)
            else -> error("Raw value from database $rawValue of class ${rawValue::class.qualifiedName} is not valid $rawClazz")
        }
    }

    override fun notNullValueToDB(value: Any) = when {
        wrapperClazz.isInstance(value) -> rawColumnType.notNullValueToDB(valueExtractor(value as Wrapper))
        else -> error("Wrapper value $value of class ${value::class.qualifiedName} is not valid $wrapperClazz")
    }

    override fun nonNullValueToString(value: Any) = when {
        wrapperClazz.isInstance(value) -> rawColumnType.nonNullValueToString(valueExtractor(value as Wrapper))
        else -> error("Raw value $value of class ${value::class.qualifiedName} is not valid $wrapperClazz")
    }

    override fun toString() =
        "WrapperColumnType(rawColumnType=$rawColumnType, rawClazz=$rawClazz, wrapperClazz=$wrapperClazz)"
}

class LongWrapperColumnType<out Wrapper : Any>(
    wrapperClazz: KClass<Wrapper>,
    instanceCreator: (Long) -> Wrapper,
    valueExtractor: (Wrapper) -> Long
) : WrapperColumnType<Long, Wrapper>(LongColumnType(), Long::class, wrapperClazz, instanceCreator, valueExtractor) {

    override fun valueFromDB(value: Any) = when (value) {
        is Long -> instanceCreator(rawColumnType.valueFromDB(value) as Long)
        is Int -> instanceCreator(rawColumnType.valueFromDB(value) as Long)
        is BigDecimal -> instanceCreator(rawColumnType.valueFromDB(value) as Long)
        else -> error("Database value $value of class ${value::class.qualifiedName} is not valid $rawClazz")
    }
}

class StringWrapperColumnType<out Wrapper : Any>(
        wrapperClazz: KClass<Wrapper>,
        instanceCreator: (String) -> Wrapper,
        valueExtractor: (Wrapper) -> String
) : WrapperColumnType<String, Wrapper>(TextColumnType(), String::class, wrapperClazz, instanceCreator, valueExtractor) {

    override fun valueFromDB(value: Any) = when (value) {
        is java.sql.Clob -> instanceCreator(rawColumnType.valueFromDB(value) as String)
        is ByteArray -> instanceCreator(rawColumnType.valueFromDB(value) as String)
        is String -> instanceCreator(rawColumnType.valueFromDB(value) as String)
        else -> error("Database value $value of class ${value::class.qualifiedName} is not valid $rawClazz")
    }

    /*
     * Essentially the same implementation as the superclass however it doesn't auto-unwrap Iterable objects
     */
    override fun valueToString(value: Any?): String = when (value) {
        null -> {
            check(nullable) { "NULL in non-nullable column" }
            "NULL"
        }
        else -> nonNullValueToString(value)
    }
}

class InstantWrapperColumnType<out Wrapper : Any>(
        wrapperClazz: KClass<Wrapper>,
        instanceCreator: (Instant) -> Wrapper,
        valueExtractor: (Wrapper) -> Instant
) : WrapperColumnType<Instant, Wrapper>(JavaInstantColumnType(), Instant::class, wrapperClazz, instanceCreator, valueExtractor) {

    override fun valueFromDB(value: Any) = when (value) {
        // Supporting same types as org.jetbrains.exposed.sql.`java-time`.JavaInstantColumnType
        is java.sql.Timestamp -> instanceCreator(rawColumnType.valueFromDB(value) as Instant)
        is String -> instanceCreator(rawColumnType.valueFromDB(value) as Instant)
        else -> error("Database value $value of class ${value::class.qualifiedName} is not valid $rawClazz")
    }
}

class BooleanWrapperColumnType<out Wrapper : Any>(
        wrapperClazz: KClass<Wrapper>,
        instanceCreator: (Boolean) -> Wrapper,
        valueExtractor: (Wrapper) -> Boolean
) : WrapperColumnType<Boolean, Wrapper>(BooleanColumnType(), Boolean::class, wrapperClazz, instanceCreator, valueExtractor) {

    // Superclass works OK
}

inline fun <reified Wrapper : Any> Table.longWrapper(
    name: String,
    noinline instanceCreator: (Long) -> Wrapper,
    noinline valueExtractor: (Wrapper) -> Long
): Column<Wrapper> = registerColumn(
    name, LongWrapperColumnType(Wrapper::class, instanceCreator, valueExtractor)
)

inline fun <reified Wrapper : Any> Table.stringWrapper(
        name: String,
        noinline instanceCreator: (String) -> Wrapper,
        noinline valueExtractor: (Wrapper) -> String
): Column<Wrapper> = registerColumn(
        name, StringWrapperColumnType(Wrapper::class, instanceCreator, valueExtractor)
)

inline fun <reified Wrapper : Any> Table.instantWrapper(
        name: String,
        noinline instanceCreator: (Instant) -> Wrapper,
        noinline valueExtractor: (Wrapper) -> Instant
): Column<Wrapper> = registerColumn(
        name, InstantWrapperColumnType(Wrapper::class, instanceCreator, valueExtractor)
)

inline fun <reified Wrapper : Any> Table.booleanWrapper(
        name: String,
        noinline instanceCreator: (Boolean) -> Wrapper,
        noinline valueExtractor: (Wrapper) -> Boolean
): Column<Wrapper> = registerColumn(
        name, BooleanWrapperColumnType(Wrapper::class, instanceCreator, valueExtractor)
)
