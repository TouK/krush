package pl.touk.exposed.one2one

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import kotlin.math.log

class EmployeeTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleOneToOne() {
        transaction {
            SchemaUtils.create(EmployeeInfoTable, EmployeeTable)

            // given
            val employee = Employee().let { employee ->
                val id = EmployeeTable.insert { it.from(employee) }[EmployeeTable.id]
                employee.copy(id = id)
            }

            val employeeInfo = EmployeeInfo(login = "admin", employee = employee).let { employeeInfo ->
                val id = EmployeeInfoTable.insert { it.from(employeeInfo) }[EmployeeInfoTable.login]
                employeeInfo.copy(login = id)
            }


            //when
            val employees = (EmployeeTable leftJoin EmployeeInfoTable).selectAll()
            employees.forEach {
                resultRow: ResultRow -> val string = resultRow.toString() }
            //then
            val fullEmployee = employee.copy(employeeInfo = employeeInfo)
//            Assertions.assertThat(employees).containsOnly(fullEmployee)
        }
    }
}