package pl.touk.exposed.converter

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class CommentTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleIdConverter() {
        transaction {
            SchemaUtils.create(CommentTable)
            val commentIdConverter = AuthorConverter()

            // given
            val comment = Comment(author = Author("John", "Smith")).let { comment ->
                val commentId = CommentTable.insert { it.from(comment) }[CommentTable.id]
                comment.copy(id = commentId)
            }

            // when
            val selectedComments = CommentTable.selectAll().toCommentList()

            // then
            Assertions.assertThat(selectedComments).containsOnly(comment)
        }
    }
}