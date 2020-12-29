package pl.touk.krush.one2one

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@Entity
data class Item(
    @Id @GeneratedValue
    val id: Long? = null,
    val size: Int
)

@Entity
data class Box(
    @Id @GeneratedValue
    val id: Long? = null,

    @OneToOne(optional = true)
    @JoinColumn(name = "box_id")
    val item: Item? = null,
    // some other data maybe
)
