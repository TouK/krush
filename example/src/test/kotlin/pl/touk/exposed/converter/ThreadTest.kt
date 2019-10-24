package pl.touk.exposed.converter

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class ThreadTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandlePropertyConverter() {
        transaction {
            SchemaUtils.create(CommentTable, ThreadTable)

            // given
            val thread = Thread(name = "Test thread").let {thread ->
                val threadId = ThreadTable.insert { it.from(thread) }[ThreadTable.threadId]
                thread.copy(threadId = threadId)
            }

            val comment = Comment(author = Author("John", "Smith"), thread = thread).let { comment ->
                val commentId = CommentTable.insert { it.from(comment) }[CommentTable.id]
                comment.copy(id = commentId)
            }

            // when
            val selectedThreads = (ThreadTable leftJoin CommentTable).selectAll().toThreadList()

            val fullThread = thread.copy(comments = listOf(comment))

            // then
            Assertions.assertThat(selectedThreads).containsOnly(fullThread)
        }
    }
}