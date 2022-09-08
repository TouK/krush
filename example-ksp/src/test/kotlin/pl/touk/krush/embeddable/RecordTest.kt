package pl.touk.krush.embeddable

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import java.time.LocalDateTime

class RecordTest: BaseDatabaseTest() {

    @Test
    fun shouldHandleEmbeddedId() {
        transaction {
            SchemaUtils.create(RecordTable)

            // given
            val recordId = RecordId("id1", "type1")
            val record = Record(recordId, LocalDateTime.now()).let(RecordTable::insert)

            // when
            val selectedRecords = RecordTable.selectAll().toRecordList()

            // then
            assertThat(selectedRecords)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsOnly(record)

        }
    }
}
