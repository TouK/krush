package pl.touk.krush.one2many.uni

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.   sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.result.*
import java.util.*

class ResultRecordTest {

    @Test
    fun shouldHandleManyToOneWithSharedKey() {
        transaction {
            SchemaUtils.create(RunTable, RunSummaryTable, ResultRecordTable)

            val runId = UUID.randomUUID().toString()
            val run = Run(runId = runId).let(RunTable::insert)
            val summary = RunSummary(runId = runId, run = run).let(RunSummaryTable::insert)

            val records = listOf(
                ResultRecord(id = RecordId(runId, "id1", RecordType.CALL), summary = summary),
                ResultRecord(id = RecordId(runId, "id2", RecordType.CALL), summary = summary)
            )

            records.forEach(ResultRecordTable::insert)

            val results = (RunSummaryTable leftJoin RunTable leftJoin ResultRecordTable)
                .selectAll().toResultRecordList()
            Assertions.assertThat(results).hasSize(2)
                .extracting(ResultRecord::summary).containsOnly(Assertions.tuple(summary))

        }
    }
}
