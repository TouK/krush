package pl.touk.exposed.one2one

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class EmployeeTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleOneToOne() {
        transaction {
            SchemaUtils.create(EmployeeInfoTable, EmployeeTable, ParkingSpotTable)

            // given
            val employee = Employee().let { employee ->
                val id = EmployeeTable.insert {}[EmployeeTable.id]
                employee.copy(id = id)
            }

            val employeeInfo = EmployeeInfo(login = "admin", employee = employee).let { employeeInfo ->
                EmployeeInfoTable.insert { it.from(employeeInfo) }
                employeeInfo.copy(employee = employee)
            }

            //when
            val employees = (EmployeeTable leftJoin EmployeeInfoTable).selectAll().toEmployeeList()

            val fullEmployee = employee.copy(employeeInfo = employeeInfo)

            //then
            Assertions.assertThat(employees).containsOnly(fullEmployee)
        }
    }

    @Test
    fun shouldHandleMultipleLevelOneToOne() {
        transaction {
            SchemaUtils.create(EmployeeInfoTable, EmployeeTable, ParkingSpotTable)

            // given
            val employee = Employee().let { employee ->
                val id = EmployeeTable.insert { it.from(employee) }[EmployeeTable.id]
                employee.copy(id = id)
            }

            val employeeInfo = EmployeeInfo(login = "admin", employee = employee).let { employeeInfo ->
                EmployeeInfoTable.insert { it.from(employeeInfo) }
                employeeInfo.copy(employee = employee)
            }

            val parkingSpot = ParkingSpot(code = "C12345", employeeInfo = employeeInfo).let { parkingSpot ->
                val id = ParkingSpotTable.insert { it.from(parkingSpot) }[ParkingSpotTable.id]
                parkingSpot.copy(id)
            }

            //when
            val employees = (EmployeeTable leftJoin EmployeeInfoTable leftJoin ParkingSpotTable).selectAll().toEmployeeList()

            val fullEmployee = employee.copy(employeeInfo = employeeInfo.copy(parkingSpot = parkingSpot))

            //then
            Assertions.assertThat(employees).containsOnly(fullEmployee)
        }
    }
}
