package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class TaskTest : BaseDatabaseTest() {

    @Test
    @Disabled("Uncomment to test M2M with real references (requires krush.references to be set to real)")
    fun shouldHandleSelfReferences() {
        transaction {
            SchemaUtils.create(TaskTable, TaskRequirementsTable)

            // given
            val task1 = TaskTable.insert(Task(description = "Collect underpants"))
            val task2 = TaskTable.insert(Task(description = "?", requirements = listOf(task1)))
            val task3 = TaskTable.insert(Task(description = "Profit!", requirements = listOf(task1, task2)))

            // when
            val selectedTasks = Join(
                    table = TaskTable,
                    otherTable = TaskRequirementsTable,
                    joinType = JoinType.LEFT,
                    onColumn = TaskTable.id,
                    otherColumn = TaskRequirementsTable.taskSourceId
            ).selectAll().toTaskList(null)

            // then
            val expectation = listOf(task1, task2, task3)

            assertThat(selectedTasks).isEqualTo(expectation)
        }
    }

}
