package pl.touk.krush.many2one

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class EmployeeTest : BaseDatabaseTest() {

    @AfterEach
    internal fun tearDown() {
        transaction {
            PhoneTable.deleteAll()
            EmployeeTable.deleteAll()
        }
    }

    @Test
    fun shouldHandleManyToOneWithEmbeddedId() {
        transaction {
            // given
            val employeeId = EmployeeId(1L, 11L)
            val employee = EmployeeTable.insert(Employee(employeeId = employeeId))
            val phone1 = PhoneTable.insert(Phone("123456789", employee = employee))
            val phone2 = PhoneTable.insert(Phone("222333444", employee = employee))

            // when
            val selectedPhones = (EmployeeTable leftJoin PhoneTable)
                .select { EmployeeTable.employeeIdEmployeeNo eq employeeId.employeeNo }
                .toPhoneList()

            // then
            assertThat(selectedPhones).containsOnly(phone1, phone2)
        }
    }

    @Test
    fun shouldHandleOneToManyWithEmbeddedId() {
        transaction {
            // given
            val employeeId = EmployeeId(1L, 11L)
            val employee = EmployeeTable.insert(Employee(employeeId = employeeId))
            val phone1 = PhoneTable.insert(Phone("123456789", employee = employee))
            val phone2 = PhoneTable.insert(Phone("222333444", employee = employee))

            // when
            val selectedEmployees = (EmployeeTable leftJoin PhoneTable)
                .select { EmployeeTable.employeeIdEmployeeNo eq employeeId.employeeNo }
                .toEmployeeList()

            // then
            assertThat(selectedEmployees).containsOnly(employee.copy(phones = listOf(phone1, phone2)))
        }
    }

}
