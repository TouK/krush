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

class EventTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleDateTypes() {
        transaction {
            SchemaUtils.create(EventTable)

            // given
            val event = Event(eventTime = DateTime.parse("2010-06-30T01:20")).let { event ->
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