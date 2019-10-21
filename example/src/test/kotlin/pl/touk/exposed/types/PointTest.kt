package pl.touk.exposed.types

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class PointTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleOneToOne() {
        transaction {
            SchemaUtils.create(PointTable)

            // given
            val point = Point(x1 = 100, y1 = 50, z1 = 0, x2 = -50.4f, y2 = 20.00004).let { point ->
                val id = PointTable.insert { it.from(point) }[PointTable.id]
                point.copy(id = id)
            }

            //when
            val points = (PointTable).selectAll().toPointList()

            //then
            Assertions.assertThat(points).containsOnly(point)
        }
    }
}