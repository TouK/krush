package pl.touk.exposed.one2many.bidi

import javax.persistence.*

@Entity
data class Customer(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "customerName", length = 100)
        val name: String,

        val age: Long,

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "customer")
        val phones: List<Phone> = emptyList(),

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "customer")
        val addresses: List<Address> = emptyList(),

        @Transient
        val currentAddress: Address? = null
)

@Entity
@Table(name = "phones")
data class Phone(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "number")
        val number: String,

        @ManyToOne
        @JoinColumn(name = "customer_id")
        val customer: Customer? = null
)

@Entity
@Table(name = "addresses")
data class Address(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "city")
        val city: String,

        @Column(name = "street")
        val street: String,

        @Column(name = "house")
        val houseNo: String,

        @Column(name = "apartment")
        val apartmentNo: String,

        @ManyToOne
        @JoinColumn(name = "customer_id")
        val customer: Customer? = null
)
