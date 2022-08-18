package pl.touk.krush.enumtype

import javax.persistence.Entity
import javax.persistence.EnumType.ORDINAL
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Message(
    @Id @GeneratedValue
    val id: Long? = null,

    @Enumerated(STRING)
    val status: MessageStatus,

    @Enumerated(ORDINAL)
    val previousStatus: MessageStatus
)

enum class MessageStatus {
    NEW, PENDING, RECEIVED
}

enum class MessagePriority(val priority: Int) {
    LOW(0), HIGH(999)
}

enum class ContentType {
    JSON, XML
}
