package pl.touk.krush.enumtype

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.krush.enumtype.ContentType.XML
import pl.touk.krush.enumtype.MessagePriority.HIGH
import pl.touk.krush.enumtype.MessageStatus.NEW
import pl.touk.krush.enumtype.MessageStatus.PENDING

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
            val message = MessageTable.insert(Message(status = PENDING, previousStatus = NEW, info = messageInfo))

            // when
            val selectedMessages = MessageTable.selectAll().toMessageList()

            // then
            assertThat(selectedMessages).containsOnly(message)
        }
    }
}
