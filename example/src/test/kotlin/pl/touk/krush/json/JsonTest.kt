package pl.touk.krush.json

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class JsonTest : BaseDatabaseTest() {

    @Test
    fun shouldPersistJsonColumn() {
        transaction {
            val reason = Reason(reason = "test", details = "\"{'field': 'value'}\"").let(ReasonTable::insert)

            val persisted = ReasonTable.selectAll().toReasonList()

            assertThat(persisted).containsOnly(reason)
        }
    }

}
