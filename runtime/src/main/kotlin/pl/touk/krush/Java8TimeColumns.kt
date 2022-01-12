package pl.touk.krush

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime
import kotlin.reflect.KClass

class ZonedDateTimeColumnType : WrapperColumnType<LocalDateTime, ZonedDateTime>(
    rawColumnType = JavaLocalDateTimeColumnType(),
    rawClazz = LocalDateTime::class,
    wrapperClazz = ZonedDateTime::class,
    instanceCreator = { it.atZone(systemDefault())},
    valueExtractor = { it.toLocalDateTime() }
)

fun Table.zonedDateTime(name: String): Column<ZonedDateTime> = registerColumn(name, ZonedDateTimeColumnType())

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

inline fun <reified Wrapper : Any> Table.instantWrapper(
    name: String,
    noinline instanceCreator: (Instant) -> Wrapper,
    noinline valueExtractor: (Wrapper) -> Instant
): Column<Wrapper> = registerColumn(
    name, InstantWrapperColumnType(Wrapper::class, instanceCreator, valueExtractor)
)
