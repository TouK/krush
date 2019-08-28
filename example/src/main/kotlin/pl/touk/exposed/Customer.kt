package pl.touk.exposed

import javax.persistence.*

@Entity
@Table(name = "customers")
data class Customer(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "name", length = 100)
        val name: String,

        @Column(name = "age")
        val age: Long
)
