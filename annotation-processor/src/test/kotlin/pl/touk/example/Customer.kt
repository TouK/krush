package pl.touk.example

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "customers")
data class Customer(
        val id: Long
)
