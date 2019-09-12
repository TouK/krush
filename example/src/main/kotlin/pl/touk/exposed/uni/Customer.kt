package pl.touk.exposed.uni

import javax.persistence.*

@Entity
@Table(name = "customers")
data class UniCustomer(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "name", length = 100)
        val name: String,

        @Column(name = "age")
        val age: Long,

        @OneToMany
        @JoinColumn(name = "customer_id")
        val phones: List<UniPhone> = emptyList(),

        @OneToMany
        @JoinColumn(name = "customer_id")
        val addresses: List<UniAddress> = emptyList()
)

@Entity
@Table(name = "phones")
data class UniPhone(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "number")
        val number: String
)

@Entity
@Table(name = "addresses")
data class UniAddress(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "city")
        val city: String,

        @Column(name = "street")
        val street: String,

        @Column(name = "house")
        val houseNo: String,

        @Column(name = "apartment")
        val apartmentNo: String
)
