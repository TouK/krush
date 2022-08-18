package pl.touk.krush.converter

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class CommentTest : BaseDatabaseTest() {

    @Test
    fun shouldHandlePropertyConverter() {
        transaction {
            SchemaUtils.create(CommentTable)

            // given
            val author = Author("John", "Smith")
            val comment = CommentTable.insert(
                Comment(
                    id = CommentId.Persisted(123),
                    author = author,
                    isVisible = true
                )
            )

            // when
            val selectedComments = CommentTable
                    .select { (CommentTable.author eq author) and (CommentTable.isVisible eq true) }
                    .toCommentList()

            // then
            assertThat(selectedComments).containsOnly(comment)
        }
    }
}
