package pl.touk.exposed.multipackage

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.exposed.multipackage.b.TestB
import pl.touk.exposed.multipackage.b.TestBTable
import pl.touk.exposed.multipackage.b.insert

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
                val id = TestATable.insert {}[TestATable.id]
                a.copy(id = id)
            }

            val b = TestBTable.insert(testB = TestB(text = "test B"), testA = a)

            // when
            val aList = (TestBTable leftJoin TestATable)
                    .select { TestBTable.text like "tes%" }
                    .toTestAList()

            // then
            assertThat(aList).containsExactly(a.copy(b = listOf(b)))
        }
    }
}
