package pl.touk.krush.types

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest
import java.math.BigDecimal
import java.math.RoundingMode

class LineTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleNumericTypes() {
        transaction {
            SchemaUtils.create(LineTable)

            // given
            val line = LineTable.insert(Line(x1 = 100, y1 = 50, z1 = 0, x2 = -50.4f, y2 = 20.00004,
                    z2 = BigDecimal(111.111).setScale(3, RoundingMode.HALF_UP)))

            //when
            val lines = (LineTable)
                    .select { LineTable.id eq line.id!! }
                    .toLineList()

            //then
            assertThat(lines).containsOnly(line)
        }
    }
}
