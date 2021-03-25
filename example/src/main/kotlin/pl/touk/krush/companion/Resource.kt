package pl.touk.krush.companion

import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id

data class JobId(val id: Long = 0) {

    override fun toString(): String = id.toString()

    companion object : AttributeConverter<JobId, Long> {
        override fun convertToDatabaseColumn(attribute: JobId): Long = attribute.id
        override fun convertToEntityAttribute(dbData: Long): JobId = JobId(dbData)
    }
}

@Entity
data class Resource(
    @Id val id: Long = 0,

    @Convert(converter = JobId.Companion::class)
    val jobId: JobId = JobId()
)
