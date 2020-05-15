package pl.touk.krush

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import java.time.LocalDate
import java.time.Month.APRIL

class BookTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleSimpleEntity() {
        transaction {
            SchemaUtils.create(BookTable)

            // given
            val book = Book(
                    isbn = "1449373321",
                    publishDate = LocalDate.of(2017, APRIL, 11),
                    title = "Designing Data-Intensive Applications",
                    author = "Martin Kleppmann"
            ).let(BookTable::insert)

            // then
            val bookId = book.id ?: throw IllegalArgumentException()
            val fetchedBook = BookTable.select { BookTable.id eq bookId }
                    .singleOrNull()?.toBook()
            assertThat(fetchedBook).isEqualTo(book)

            // when
            val selectedBooks = BookTable
                    .select { BookTable.author like "Martin K%" }
                    .toBookList()

            // then
            assertThat(selectedBooks).containsOnly(book)
        }
    }
}
