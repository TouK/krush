package pl.touk.krush.types

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
data class Event(

    @Id
    @GeneratedValue
    val id: Long? = null,

        val eventDate: LocalDate,
        val processTime: LocalDateTime,
        val createTime: ZonedDateTime,
        val updateTime: Instant,

        @Convert(converter = ExampleInstantWrapperAttributeConverter::class)
        val otherUpdateTime: ExampleInstantWrapper,

        val externalId: UUID
)

data class ExampleInstantWrapper(val value: Instant)

class ExampleInstantWrapperAttributeConverter : AttributeConverter<ExampleInstantWrapper, Instant> {
    override fun convertToDatabaseColumn(attribute: ExampleInstantWrapper): Instant {
        return attribute.value
    }

    override fun convertToEntityAttribute(dbData: Instant): ExampleInstantWrapper {
        return ExampleInstantWrapper(dbData)
    }
}
