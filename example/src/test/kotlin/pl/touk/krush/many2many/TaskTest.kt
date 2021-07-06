package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class TaskTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleSelfReferences() {
        transaction {
            SchemaUtils.create(BoardTable, TaskTable, TaskRequirementsTable)

            // given
            val board = BoardTable.insert(Board())
            val task1 = TaskTable.insert(Task(description = "Collect underpants"), board = board)
            val task2 = TaskTable.insert(Task(description = "?", requirements = listOf(task1)), board = board)
            val task3 = TaskTable.insert(Task(description = "Profit!", requirements = listOf(task1, task2)), board = board)

            // when
            val selectedBoards = Join(
                table = BoardTable,
                otherTable = TaskTable,
                joinType = JoinType.LEFT,
                onColumn = BoardTable.id,
                otherColumn = TaskTable.boardId
            ).join(
                otherTable = TaskRequirementsTable,
                joinType = JoinType.LEFT,
                onColumn = TaskTable.id,
                otherColumn = TaskRequirementsTable.taskSourceId
            ).selectAll().toBoardList()

            // then
            assertThat(selectedBoards).hasSize(1)
                .allSatisfy {
                    assertThat(it.tasks)
                        .containsExactlyInAnyOrder(task1, task2, task3)
                }
        }
    }

}
