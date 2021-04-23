package pl.touk.krush.one2one

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class EmployeeTest : BaseDatabaseTest() {

    @AfterEach
    internal fun tearDown() {
        transaction {
            ParkingSpotTable.deleteAll()
            EmployeeInfoTable.deleteAll()
            EmployeeTable.deleteAll()
        }
    }

    @Test
    fun shouldHandleOneToOne() {
        transaction {
            SchemaUtils.create(EmployeeInfoTable, EmployeeTable, ParkingSpotTable)

            // given
            val employee = EmployeeTable.insert(Employee())
            val employeeInfo = EmployeeInfoTable.insert(EmployeeInfo(login = "admin", employee = employee))

            // when
            val employees = (EmployeeTable leftJoin EmployeeInfoTable)
                    .select { EmployeeInfoTable.login.regexp("[a-z]{5}") }
                    .toEmployeeList()

            val fullEmployee = employee.copy(employeeInfo = employeeInfo)

            //t hen
            assertThat(employees).containsOnly(fullEmployee)
        }
    }

    @Test
    fun shouldHandleMultipleLevelOneToOne() {
        transaction {
            SchemaUtils.create(EmployeeInfoTable, EmployeeTable, ParkingSpotTable)

            // given
            val employee = EmployeeTable.insert(Employee())
            val employeeInfo = EmployeeInfoTable.insert(EmployeeInfo(login = "admin", employee = employee))
            val parkingSpot = ParkingSpotTable.insert(ParkingSpot(code = "C12345", employeeInfo = employeeInfo))

            // when
            val employees = (EmployeeTable leftJoin EmployeeInfoTable leftJoin ParkingSpotTable).selectAll().toEmployeeList()
            val fullEmployee = employee.copy(employeeInfo = employeeInfo.copy(parkingSpot = parkingSpot))

            // then
            assertThat(employees).containsOnly(fullEmployee)
        }
    }
}
