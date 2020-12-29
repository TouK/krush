package pl.touk.krush.one2one

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class ItemTest : BaseDatabaseTest()  {

    @Test
    fun shouldHandleOptionalOneToOne() {
        transaction {
            SchemaUtils.create(ItemTable, BoxTable)

            // given
            val item = Item(size = 2).let(ItemTable::insert)

            // when
            val boxWithItem = Box(item = item).let(BoxTable::insert)
            val emptyBox = Box().let(BoxTable::insert)

            // then
            val boxes = (BoxTable leftJoin ItemTable).selectAll().toBoxList()
            assertThat(boxes).containsExactlyInAnyOrder(boxWithItem, emptyBox)
        }
    }
}
