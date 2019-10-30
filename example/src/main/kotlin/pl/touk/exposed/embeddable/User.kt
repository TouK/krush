package pl.touk.exposed.embeddable

import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class User(

        @Id @GeneratedValue
        val id: Long? = null,

        @Embedded
        val contactAddress: Address.ContactAddress

//        @Embedded
//        val invoiceAddress: Address.InvoiceAddress
)

sealed class Address {

        @Embeddable
        data class ContactAddress(

                val city: String,

                @Column
                val street: String,

                @Column(name = "houseNo")
                val houseNumber: Int
        ) : Address()

//        @Embeddable
//        data class InvoiceAddress(
//
//                @Column(name = "inv_city")
//                val city: String,
//
//                @Column(name = "inv_street")
//                val street: String,
//
//                @Column(name = "inv_houseNo")
//                val houseNumber: Int
//        ) : Address()
}
