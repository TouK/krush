package pl.touk.example

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "customers")
data class Customer(

        @Id @GeneratedValue
        val id: Long?,

        @Column(name = "name")
        val name: String
)
