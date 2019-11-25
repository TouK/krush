package pl.touk.krush.enumtype

import javax.persistence.Embeddable
import javax.persistence.Embedded
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
        val previousStatus: MessageStatus,

        @Embedded
        val info: MessageInfo
)

@Embeddable
data class MessageInfo(

        @Enumerated(STRING)
        val contentType: ContentType,

        @Enumerated(ORDINAL)
        val priority: MessagePriority
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
