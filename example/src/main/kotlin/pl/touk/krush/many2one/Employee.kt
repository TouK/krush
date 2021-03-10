package pl.touk.krush.many2one

import javax.persistence.*

@Embeddable
data class EmployeeId(

    @Column(name = "company_id")
    val companyId: Long,

    @Column(name = "employee_no")
    val employeeNo: Long
)

@Entity
data class Employee(
    @EmbeddedId
    val employeeId: EmployeeId,

    @OneToMany(mappedBy = "employee")
    val phones: List<Phone> = emptyList()
)

@Entity
data class Phone(
    @Id
    @Column(name = "phone_no")
    val number: String,

    @ManyToOne
    @JoinColumns(
        JoinColumn(name = "company_id"),
        JoinColumn(name = "employee_no")
    )
    val employee: Employee

)
