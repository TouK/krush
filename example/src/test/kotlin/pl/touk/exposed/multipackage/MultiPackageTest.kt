package pl.touk.exposed.multipackage

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.exposed.multipackage.b.TestB
import pl.touk.exposed.multipackage.b.TestBTable
import pl.touk.exposed.multipackage.b.from

class MultiPackageTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }


    @Test
    fun shouldHandleMultiPackageClasses() {
        transaction {
            SchemaUtils.create(TestATable, TestBTable)

            // given
            val a = TestA().let { a ->
                val id = TestATable.insert { it.from(a) }[TestATable.id]
                a.copy(id = id)
            }

            val b = TestB(text = "test B").let { b ->
                val id = TestBTable.insert { it.from(b, a) }[TestBTable.id]
                b.copy(id = id)

            }

            // when
            val aList = (TestBTable leftJoin TestATable).selectAll().toTestAList()

            // then
            assertThat(aList).containsExactly(a.copy(b = listOf(b)))
        }
    }
}
