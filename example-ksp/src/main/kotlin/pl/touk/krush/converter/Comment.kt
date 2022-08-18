package pl.touk.krush.converter

import pl.touk.krush.common.IdNotPersistedDelegate
import pl.touk.krush.common.RefId
import javax.persistence.*

@Entity
data class Comment(
    @Id
    @Convert(converter = CommentIdConverter::class)
    val id: CommentId = CommentId.New,

    @Convert(converter = AuthorConverter::class)
    val author: Author,

    val isVisible: Boolean
)

sealed class CommentId : RefId<Long>() {
    object New : CommentId() {
        override val value: Long by IdNotPersistedDelegate<Long>()
    }

    data class Persisted(override val value: Long) : CommentId() {
        override fun toString() = "CommentId(value=$value)"
    }
}

data class Author(val name: String, val surname: String)

class AuthorConverter : AttributeConverter<Author, String> {

    override fun convertToDatabaseColumn(attribute: Author): String {
        return attribute.name.plus(" ").plus(attribute.surname)
    }

    override fun convertToEntityAttribute(dbData: String): Author {
        val (name, surname) = dbData.split(" ")
        return Author(name, surname)
    }
}

class CommentIdConverter : AttributeConverter<CommentId, Long> {

    override fun convertToDatabaseColumn(attribute: CommentId): Long {
        return attribute.value
    }

    override fun convertToEntityAttribute(dbData: Long): CommentId {
        return CommentId.Persisted(dbData)
    }

}
