package pl.touk.krush.many2one

import javax.persistence.*

@Entity
data class Customer(
    @Id @GeneratedValue
    val id: Long? = null,

    val name: String,

    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    val contactPerson: ContactPerson
)

@Entity
data class ContactPerson(
    @Id @GeneratedValue
    val id: Long? = null,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String
)
