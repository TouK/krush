package pl.touk.example

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "customers")
data class Customer(
        val id: Long,

        @Column(name = "name")
        val name: String
)
