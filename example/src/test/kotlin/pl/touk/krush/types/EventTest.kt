package pl.touk.krush.types

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime
import java.util.UUID.randomUUID


class EventTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleUUIDAndDateTypes() {
        transaction {
            SchemaUtils.create(EventTable)

            // given
            val clock = Clock.fixed(Instant.parse("2019-10-22T09:00:00.000Z"), systemDefault())

            val createTime = ZonedDateTime.now(clock)
            val event = EventTable.insert(Event(eventDate = LocalDate.now(clock), processTime = LocalDateTime.now(clock),
                    createTime = createTime, externalId = randomUUID()))

            //when
            val events = (EventTable)
                    .select { EventTable.createTime greater createTime.minusDays(1) }
                    .toEventList()

            //then
            assertThat(events).containsOnly(event)
        }
    }
}
