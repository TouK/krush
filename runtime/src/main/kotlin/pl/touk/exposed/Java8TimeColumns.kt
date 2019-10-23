package pl.touk.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.DateColumnType
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTimeColumnType : WrapperColumnType<DateTime, ZonedDateTime>(
    rawColumnType = DateColumnType(time = true),
    rawClazz = DateTime::class,
    wrapperClazz = ZonedDateTime::class,
    instanceCreator = { it.toZonedDateTime() },
    valueExtractor = { it.toDateTime() }
)

class LocalDateTimeColumnType : WrapperColumnType<DateTime, LocalDateTime>(
    rawColumnType = DateColumnType(time = true),
    rawClazz = DateTime::class,
    wrapperClazz = LocalDateTime::class,
    instanceCreator = { it.toJava8LocalDateTime() },
    valueExtractor = { it.toDateTime() }
)

fun Table.zonedDateTime(name: String): Column<ZonedDateTime> = registerColumn(name, ZonedDateTimeColumnType())

fun Table.localDateTime(name: String): Column<LocalDateTime> = registerColumn(name, LocalDateTimeColumnType())

/**
 * This class uses implementation of methods from given column type, presumably a date column.
 * The only difference is prefixing date with `timestamp` Oracle keyword to use this type while comparing dates.
 */
abstract class ColumnTypeWithTimestampLiteral(private val original: ColumnType) : ColumnType() {
    override fun sqlType(): String = original.sqlType()

    override fun nonNullValueToString(value: Any): String =
        "timestamp ${original.nonNullValueToString(value)}"

    override fun valueFromDB(value: Any): Any = original.valueFromDB(value)

    override fun notNullValueToDB(value: Any): Any = original.notNullValueToDB(value)
}

class ZonedDateTimeColumnTypeWithTimestampLiteral : ColumnTypeWithTimestampLiteral(ZonedDateTimeColumnType())

private fun ZonedDateTime.toDateTime() = DateTime(toInstant().toEpochMilli())

private fun LocalDateTime.toDateTime() = atZone(ZoneId.systemDefault()).toDateTime()

private fun DateTime.toZonedDateTime() = toGregorianCalendar().toZonedDateTime()

private fun DateTime.toJava8LocalDateTime() = toZonedDateTime().toLocalDateTime()
