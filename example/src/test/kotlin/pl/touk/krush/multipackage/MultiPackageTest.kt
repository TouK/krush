package pl.touk.krush.multipackage

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import pl.touk.krush.multipackage.b.TestB
import pl.touk.krush.multipackage.b.TestBTable
import pl.touk.krush.multipackage.b.insert

class MultiPackageTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleMultiPackageClasses() {
        transaction {
            SchemaUtils.create(TestATable, TestBTable)

            // given
            val a = TestA().let { a ->
                val id = TestATable.insert {}[TestATable.id]
                a.copy(id = id)
            }

            val b = TestBTable.insert(testB = TestB(text = "test B"), testAParam = a)

            // when
            val aList = (TestBTable leftJoin TestATable)
                    .select { TestBTable.text like "tes%" }
                    .toTestAList()

            // then
            assertThat(aList).containsExactly(a.copy(b = listOf(b)))
        }
    }
}
