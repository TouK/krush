package pl.touk.krush

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.AUGUST
import java.time.Month.NOVEMBER

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

            // then
            val selectedBooks = BookTable.selectAll().toBookList()
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

            // then
            val selectedBooks = BookTable.selectAll().toBookList()
            assertThat(selectedBooks).containsOnly(book)
        }
    }

    @Test
    fun shouldBatchInsert() {
        transaction {
            SchemaUtils.create(BookTable)

            // given
            val got1 = Book(
                isbn = "0-553-10354-7",
                publishDate = LocalDate.of(1996, AUGUST, 1),
                title = "A Game of Thrones",
                author = "George R. R. Martin"
            )

            val got2 = Book(
                isbn = "0-553-10803-4",
                publishDate = LocalDate.of(1998, NOVEMBER, 16),
                title = "A Clash of Kings",
                author = "George R. R. Martin"
            )

            // when
            BookTable.batchInsert(listOf(got1, got2), body = { book -> this.from(book) })

            // then
            val selectedBooks = BookTable.selectAll().toBookList()
            assertThat(selectedBooks)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactly(got1, got2)
        }
    }

    @Test
    fun shouldBatchReplace() {
        transaction {
            SchemaUtils.create(BookTable)

            // given
            val got1 = Book(
                isbn = "0-553-10354-7",
                publishDate = LocalDate.of(1996, AUGUST, 1),
                title = "A Game of Thrones",
                author = "George R. R. Martin"
            )

            val got2 = Book(
                isbn = "0-553-10803-4",
                publishDate = LocalDate.of(1998, NOVEMBER, 16),
                title = "A Clash of Kings",
                author = "George R. R. Martin"
            )

            // when
            BookTable.batchReplace(
                listOf(got1 to 1, got2 to 2, got2 to 2),
                body = { (book, id) ->
                    this[BookTable.id] = id
                    this.from(book)
                })

            // then
            val selectedBooks = BookTable.selectAll().toBookList()
            assertThat(selectedBooks)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactly(got1, got2)
        }
    }
}
