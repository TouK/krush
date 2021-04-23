package pl.touk.krush.json

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "REASONS")
data class Reason(
    @GeneratedValue
    @Id
    val id: Long? = null,

    @Column(name = "REASON")
    val reason: String,

    @Column(name = "DETAILS", columnDefinition = "jsonb")
    val details: String
)
