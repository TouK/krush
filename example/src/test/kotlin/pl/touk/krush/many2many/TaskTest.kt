package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import kotlin.reflect.KClass

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

data class RowWrapper(
    var row: ResultRow,
    val entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf(),
    val selfReferenceRequests: MutableMap<KClass<*>, MutableMap<Any, MutableSet<Any>>> = mutableMapOf()
)

fun ResultRow.toBoard(entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()): Board {
    val id = this[BoardTable.id]
    val cacheMap = entityStore.getOrPut(Board::class) { mutableMapOf() }
    if (cacheMap[id] != null) {
        return cacheMap[id] as Board
    }
    val result = Board(
        id = id,
        tasks = mutableListOf()
    )
    cacheMap[id] = result
    return result
}

fun RowWrapper.toBoard() = this.row.toBoard(this.entityStore)

fun ResultRow.toTask(entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()): Task {
    val id = this[TaskTable.id]
    val cacheMap = entityStore.getOrPut(Task::class) { mutableMapOf() }
    if (cacheMap[id] != null) {
        return cacheMap[id] as Task
    }
    val result = Task(
        id = id,
        description = this[TaskTable.description],
        requirements = mutableListOf()
    )
    cacheMap[id] = result
    return result
}

fun RowWrapper.toTask() = this.row.toTask(this.entityStore)

fun Iterable<ResultRow>.toBoardList(): List<Board> = this.toBoardMap().values.toList()

fun RowWrapper.addSubEntitiesToBoard(board: Board?): Unit {
    if(board == null) return
    val tasksId = this.row.getOrNull(TaskTable.id)
    if(tasksId != null) {
        val tasksAttr = board.tasks as MutableList<Task>
        val tasksAttrLastElement = tasksAttr.lastOrNull()
        if(tasksId != tasksAttrLastElement?.id) {
            // If the sub-entity is new, create a new object for it
            val newTask = toTask()
            addSubEntitiesToTask(newTask)
            tasksAttr.add(newTask)
        } else {
            // If we already have an entity with this ID, check if there's a new sub-sub-entity in it
            addSubEntitiesToTask(tasksAttrLastElement)
        }
    }
}

fun RowWrapper.addSubEntitiesToTask(task: Task?): Unit {
    if(task == null) return
    val otherTaskId = this.row.getOrNull(TaskRequirementsTable.taskTargetId)
    if(otherTaskId != null) {
        val taskSelfReferenceRequests = selfReferenceRequests.getOrPut(Task::class) { mutableMapOf() }
        val otherTasks = taskSelfReferenceRequests.getOrPut(otherTaskId) { mutableSetOf() }
        otherTasks.add(task.id!!)
    }
}

fun Iterable<ResultRow>.toBoardMap(): Map<Long, Board> {
    val entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()
    val selfReferenceRequests: MutableMap<KClass<*>, MutableMap<Any, MutableSet<Any>>> = mutableMapOf()
    this.forEach { row ->
        // Create this entity or expand on the sub-entity lists contained within
        val rowWrapper = RowWrapper(row, entityStore, selfReferenceRequests)
        val currentBoard = rowWrapper.toBoard()
        rowWrapper.addSubEntitiesToBoard(currentBoard)
    }
    selfReferenceRequests.forEach { (clazz, unsatisfiedMap) ->
        when(clazz) {
            Task::class -> unsatisfiedMap.forEach { (subjectTaskId, referencingTaskIds) ->
                val subjectTask = entityStore[Task::class]?.get(subjectTaskId) as? Task
                subjectTask?.let {
                    referencingTaskIds.forEach { referencingTaskId ->
                        val referencingTask = entityStore[Task::class]?.get(referencingTaskId) as? Task
                        (referencingTask?.requirements as? MutableList<Task>)?.add(subjectTask)
                    }
                }
            }
        }
    }
    return (entityStore[Board::class] ?: emptyMap()) as Map<kotlin.Long, Board>
}

