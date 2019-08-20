package pl.touk.exposed

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "customers")
data class Customer(
    @Id
    @GeneratedValue
    val id: Long
)
