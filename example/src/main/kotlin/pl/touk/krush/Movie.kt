package pl.touk.krush

import kotlinx.serialization.Serializable
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "books")
@Serializable
data class Movie(
    @Id @GeneratedValue
    val id: Int? = null,

    val title: String
)
