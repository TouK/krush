package pl.touk.krush.converter

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class ThreadTest : BaseDatabaseTest() {

    @Test
    fun shouldHandlePropertyConverter() {
        transaction {
            SchemaUtils.create(CommentTable, ThreadTable)

            // given
            val thread = ThreadTable.insert(Thread(name = "Test thread"))

            val author = Author("John", "Smith")
            val comment = CommentTable.insert(
                    Comment(
                            id = CommentId.Persisted(123),
                            author = author,
                            thread = thread,
                            isVisible = StringBoolean("true")
                    )
            )

            // when
            val selectedThreads = (ThreadTable leftJoin CommentTable)
                    .select { (CommentTable.author eq author) and (CommentTable.isVisible eq StringBoolean("true")) }
                    .toThreadList()

            val fullThread = thread.copy(comments = listOf(comment))

            // then
            assertThat(selectedThreads).containsOnly(fullThread)
        }
    }
}
