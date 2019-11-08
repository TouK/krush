package pl.touk.exposed.converter

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
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
            val thread = ThreadTable.insert(Thread(name = "Test thread"))

            val author = Author("John", "Smith")
            val comment = CommentTable.insert(Comment(author = author, thread = thread))

            // when
            val selectedThreads = (ThreadTable leftJoin CommentTable)
                    .select { CommentTable.author eq author }
                    .toThreadList()

            val fullThread = thread.copy(comments = listOf(comment))

            // then
            Assertions.assertThat(selectedThreads).containsOnly(fullThread)
        }
    }
}
