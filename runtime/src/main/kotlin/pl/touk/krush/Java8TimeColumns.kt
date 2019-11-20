package pl.touk.krush

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime

class ZonedDateTimeColumnType : WrapperColumnType<LocalDateTime, ZonedDateTime>(
    rawColumnType = JavaLocalDateTimeColumnType(),
    rawClazz = LocalDateTime::class,
    wrapperClazz = ZonedDateTime::class,
    instanceCreator = { it.atZone(systemDefault())},
    valueExtractor = { it.toLocalDateTime() }
)

fun Table.zonedDateTime(name: String): Column<ZonedDateTime> = registerColumn(name, ZonedDateTimeColumnType())
