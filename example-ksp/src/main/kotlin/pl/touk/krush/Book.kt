package pl.touk.krush

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "books")
data class Book(
    @Id @GeneratedValue
    val id: Int? = null,
    @Column(name = "ISBN")
    val isbn: String,
    val author: String,
    val title: String,
    val publishDate: LocalDate
)
