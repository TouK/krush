package pl.touk.exposed.enumtype

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.exposed.enumtype.ContentType.XML
import pl.touk.exposed.enumtype.MessagePriority.HIGH
import pl.touk.exposed.enumtype.MessageStatus.NEW
import pl.touk.exposed.enumtype.MessageStatus.PENDING

class MessageTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleEnumTypes() {
        transaction {
            SchemaUtils.create(MessageTable)

            // given
            val messageInfo = MessageInfo(contentType = XML, priority = HIGH)
            val message = Message(status = PENDING, previousStatus = NEW, info = messageInfo)
                    .let { message ->
                        val messageId = MessageTable.insert { it.from(message) }[MessageTable.id]
                        message.copy(id = messageId)
                    }

            // when
            val selectedMessages = MessageTable.selectAll().toMessageList()

            // then
            assertThat(selectedMessages).containsOnly(message)
        }
    }
}
