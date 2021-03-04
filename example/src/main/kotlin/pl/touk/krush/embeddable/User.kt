package pl.touk.krush.embeddable

import javax.persistence.*

@Entity
data class User(
    @Id @GeneratedValue
    val id: Long? = null,

    @Embedded
    @AttributeOverride(name = "houseNumber", column = Column(name = "house_no"))
    val contactAddress: Address,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "city", column = Column(name = "inv_city")),
        AttributeOverride(name = "street", column = Column(name = "inv_street")),
        AttributeOverride(name = "houseNumber", column = Column(name = "inv_house_no")),
        AttributeOverride(name = "apartmentNumber", column = Column(name = "inv_apartment_no"))
    )
    val invoiceAddress: Address? = null
)

@Embeddable
data class Address(

    @Column(name = "city")
    val city: String,

    @Column(name = "street")
    val street: String,

    @Column(name = "house_number")
    val houseNumber: Int,

    @Column(name = "apartment_number")
    val apartmentNumber: Int? = null
)
