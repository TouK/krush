package pl.touk.krush.embeddable

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Embeddable
data class RecordId(
    @Column(name = "ID")
    val id: String,

    @Column(name = "TYPE")
    val type: String
) {
    companion object {
        fun from(id: String, type: String) = RecordId(id, type)
    }
}

@Entity
data class Record(

    @EmbeddedId
    val id: RecordId,

    val timestamp: LocalDateTime

)
