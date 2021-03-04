package pl.touk.krush.one2one

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class RunSummaryTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleRequiredOneToOne() {
        transaction {
            SchemaUtils.create(RunTable, RunSummaryTable, ResultRecordTable)

            val run = Run()
            val summary = RunSummary(run = run, records = emptyList()).let {
                it.copy(
                    records = listOf(
                        ResultRecord(recordId = RecordId("id1", RecordType.CALL), summary = it),
                        ResultRecord(recordId = RecordId("id2", RecordType.CALL), summary = it)
                    )
                )
            }

            val savedRun = run.let(RunTable::insert)
            val savedSummary = RunSummaryTable.insert(summary.copy(run = savedRun))
            summary.records.forEach { record ->
                ResultRecordTable.insert(record.copy(summary = savedSummary))
            }

            val results = (RunSummaryTable leftJoin RunTable leftJoin ResultRecordTable)
                .selectAll().toRunSummaryList()

            assertThat(results).hasSize(1)
                .extracting(RunSummary::run).containsExactly(tuple(savedRun))
            assertThat(results)
                .flatExtracting("records")
                .extracting("recordId")
                .containsExactly(
                    RecordId("id1", RecordType.CALL),
                    RecordId("id2", RecordType.CALL)
                )

        }
    }
}
