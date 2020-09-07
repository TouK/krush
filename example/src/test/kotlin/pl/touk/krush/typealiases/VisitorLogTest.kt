package pl.touk.krush.typealiases

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import pl.touk.krush.types.Event
import pl.touk.krush.types.EventTable
import pl.touk.krush.types.insert
import pl.touk.krush.types.toEventList
import java.time.*
import java.util.*

class VisitorLogTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleTypeAliases() {
        transaction {
            SchemaUtils.create(VisitorLogTable)

            // given
            val log = VisitorLogTable.insert(VisitorLog(visitors = listOf("Krush", "Kotlin", "Gradle" ), guard = "Kelly"))

            //when
            val logs = (VisitorLogTable)
                    .select { VisitorLogTable.guard eq "Kelly" }
                    .toVisitorLogList()

            //then
            Assertions.assertThat(logs).containsOnly(log)
        }
    }
}
