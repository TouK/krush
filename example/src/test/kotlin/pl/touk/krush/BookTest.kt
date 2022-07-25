package pl.touk.krush

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import java.time.LocalDate
import java.time.Month.APRIL

class BookTest : BaseDatabaseTest() {

    @AfterEach
    internal fun tearDown() {
        transaction {
            BookTable.deleteAll()
        }
    }

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

    @Test
    fun shouldUpdate() {
        transaction {
            SchemaUtils.create(BookTable)

            // given
            val book = Book(
                isbn = "1449373321",
                publishDate = LocalDate.of(2017, APRIL, 11),
                title = "Designing Data-Intensive Applications",
                author = "Martin Kleppmann"
            ).let(BookTable::insert)

            val bookId = book.id ?: throw IllegalArgumentException()

            // when
            val updated = book.copy(publishDate = LocalDate.of(2021, APRIL, 11))
            BookTable.update({ BookTable.id eq bookId }, body = { it.from(updated) })

            val selectedBooks = BookTable.selectAll().toBookList()

            // then
            assertThat(selectedBooks).containsOnly(updated)
        }
    }

    @Test
    fun shouldReplace() {
        transaction {
            SchemaUtils.create(BookTable)

            // given
            val book = Book(
                isbn = "1449373321",
                publishDate = LocalDate.of(2017, APRIL, 11),
                title = "Designing Data-Intensive Applications",
                author = "Martin Kleppmann"
            ).let(BookTable::insert)

            val bookId = book.id ?: throw IllegalArgumentException()

            // when
            BookTable.replace {
                it[id] = bookId
                it.from(book)
            }
            BookTable.replace {
                it[id] = bookId
                it.from(book)
            }

            val selectedBooks = BookTable.selectAll().toBookList()

            // then
            assertThat(selectedBooks).containsOnly(book)
        }
    }
}
