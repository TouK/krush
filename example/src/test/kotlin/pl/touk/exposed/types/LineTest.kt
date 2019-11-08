package pl.touk.exposed.types

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class LineTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

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
            Assertions.assertThat(lines).containsOnly(line)
        }
    }
}
