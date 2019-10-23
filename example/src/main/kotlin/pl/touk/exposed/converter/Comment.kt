package pl.touk.exposed.converter

import pl.touk.exposed.Convert
import pl.touk.exposed.Converter
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Comment(

        @Id @GeneratedValue
        @Convert(value = IdConverter::class)
        val id: String? = null,

        @Convert(value = AuthorConverter::class)
        val author: Author
)

data class Author(
        val name: String,
        val surname: String
)

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