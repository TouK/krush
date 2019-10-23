package pl.touk.exposed.types

import org.joda.time.DateTime
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

    val eventTime: DateTime,
    val processTime: LocalDateTime,
    val createTime: ZonedDateTime,
    val externalId: UUID
)