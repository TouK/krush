package pl.touk.krush.types

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Event(

    @Id
    @GeneratedValue
    val id: Long? = null,

    val eventDate: LocalDate,
    val processTime: LocalDateTime,
    val createTime: ZonedDateTime,
    val externalId: UUID
)
