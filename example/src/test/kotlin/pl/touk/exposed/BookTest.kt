package pl.touk.exposed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.Month.APRIL

class BookTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleSimpleEntity() {
        transaction {
            SchemaUtils.create(BookTable)

            //given
            val book = BookTable.insert(Book(isbn = "1449373321", publishDate = LocalDate.of(2017, APRIL, 11),
                    title = "Designing Data-Intensive Applications", author = "Martin Kleppmann"))

            // when
            val selectedBooks = (BookTable)
                    .select { BookTable.author like "Martin K%" }
                    .toBookList()

            // then
            assertThat(selectedBooks).containsOnly(book)
        }
    }
}
