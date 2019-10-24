package pl.touk.exposed.converter

import pl.touk.exposed.Convert
import pl.touk.exposed.Converter
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import kotlin.reflect.KProperty

@Entity
data class Thread(

        @Id @GeneratedValue
        @Convert(value = ThreadConverter::class)
        val id: ThreadId = ThreadId.New,

        val name: String,

        @OneToMany(mappedBy = "thread")
        val comments: List<Comment> = emptyList()
)

sealed class ThreadId : RefId<Long>() {
    object New : ThreadId() {
        override val value: Long by IdNotPersistedDelegate<Long>()
    }

    data class Persisted(override val value: Long) : ThreadId() {
        override fun toString() = "ThreadId(value=$value)"
    }
}

@Entity
data class Comment(

        @Id @GeneratedValue
        @Convert(value = IdConverter::class)
        val id: String? = null,

        @Convert(value = AuthorConverter::class)
        val author: Author,

        @ManyToOne
        @JoinColumn(name = "thread_id")
        val thread: Thread? = null
)

data class Author(

        val name: String,
        val surname: String
)

abstract class RefId<T : Comparable<T>> : Comparable<RefId<T>> {
    abstract val value: T

    override fun compareTo(other: RefId<T>) = value.compareTo(other.value)
}

class IdNotPersistedDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Nothing = throw IllegalStateException("Id not persisted yet")
}

class AuthorConverter : Converter<Author, String> {

    override fun convertToDatabaseColumn(attribute: Author): String {
        return attribute.name.plus(" ").plus(attribute.surname)
    }

    override fun convertToEntityAttribute(dbData: String): Author {
        val (name, surname) = dbData.split(" ")
        return Author(name, surname)
    }
}

class IdConverter : Converter<String, Long> {

    override fun convertToDatabaseColumn(attribute: String): Long {
        return attribute.toLong()
    }

    override fun convertToEntityAttribute(dbData: Long): String {
        return dbData.toString()
    }

}

class ThreadConverter : Converter<ThreadId, Long> {

    override fun convertToDatabaseColumn(attribute: ThreadId): Long {
        return attribute.value
    }

    override fun convertToEntityAttribute(dbData: Long): ThreadId {
        return ThreadId.Persisted(dbData)
    }
}