package pl.touk.krush.embeddable

import pl.touk.krush.embeddable.Address.*
import javax.persistence.AttributeOverride
import javax.persistence.AttributeOverrides
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
    @AttributeOverride(name = "houseNumber", column = Column(name = "house_no"))
    val contactAddress: ContactAddress,

    @Embedded
    @AttributeOverrides(
            AttributeOverride(name = "city", column = Column(name = "inv_city")),
            AttributeOverride(name = "street", column = Column(name = "inv_street"))
    )
    val invoiceAddress: InvoiceAddress
)

sealed class Address {

    @Embeddable
    data class ContactAddress(

            val city: String,

            @Column
            val street: String,

            val houseNumber: Int
    ) : Address()

    @Embeddable
    data class InvoiceAddress(

            val city: String,

            val street: String,

            val houseNumber: Int
    ) : Address()
}
