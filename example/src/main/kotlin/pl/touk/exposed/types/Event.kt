package pl.touk.exposed.types

import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Event(

    @Id
    @GeneratedValue
    val id: Long? = null,

    val eventTime: DateTime
)