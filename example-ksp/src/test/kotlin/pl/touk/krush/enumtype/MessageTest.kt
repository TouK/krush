package pl.touk.krush.enumtype

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import pl.touk.krush.enumtype.MessageStatus.NEW
import pl.touk.krush.enumtype.MessageStatus.PENDING

class MessageTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleEnumTypes() {
        transaction {
            SchemaUtils.create(MessageTable)

            // given
            val message = MessageTable.insert(Message(status = PENDING, previousStatus = NEW))

            // when
            val selectedMessages = MessageTable.selectAll().toMessageList()

            // then
            assertThat(selectedMessages).containsOnly(message)
        }
    }
}
