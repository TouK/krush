package pl.touk.exposed.one2one

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@Entity
data class Employee(
        @Id @GeneratedValue
        val id: Long? = null,

        @OneToOne(mappedBy = "employee")
        val employeeInfo: EmployeeInfo? = null
)

@Entity
data class EmployeeInfo(
        @Id
        val login: String,

        @OneToOne
        @JoinColumn(name = "employee_id")
        val employee: Employee? = null
)