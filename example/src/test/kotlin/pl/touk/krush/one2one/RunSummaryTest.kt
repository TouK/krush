package pl.touk.krush.one2one

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import pl.touk.krush.result.*
import java.util.*

class RunSummaryTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleOneToOneWithSharedKey() {
        transaction {
            SchemaUtils.create(RunTable, RunSummaryTable, ResultRecordTable)

            val runId = UUID.randomUUID().toString()
            val run = Run(runId = runId).let(RunTable::insert)
            RunSummary(runId = runId, run = run).let(RunSummaryTable::insert)

            val results = (RunSummaryTable leftJoin RunTable)
                .selectAll().toRunSummaryList()

            assertThat(results).hasSize(1)
                .extracting(RunSummary::run).containsExactly(tuple(run))
        }
    }
}
