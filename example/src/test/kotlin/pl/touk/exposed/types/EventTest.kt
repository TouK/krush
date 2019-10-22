package pl.touk.exposed.types

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime
import java.util.UUID.randomUUID

class EventTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleUUIDAndDateTypes() {
        transaction {
            SchemaUtils.create(EventTable)

            // given
            val clock = Clock.fixed(Instant.parse("2019-10-22T09:00:00.000Z"), systemDefault())

            val event = Event(
                    eventTime = DateTime.parse("2019-10-22T09:00"), processTime = LocalDateTime.now(clock),
                    createTime = ZonedDateTime.now(clock), externalId = randomUUID()).let { event ->
                val id = EventTable.insert { it.from(event) }[EventTable.id]
                event.copy(id = id)
            }

            //when
            val events = (EventTable).selectAll().toEventList()

            //then
            Assertions.assertThat(events).containsOnly(event)
        }
    }
}