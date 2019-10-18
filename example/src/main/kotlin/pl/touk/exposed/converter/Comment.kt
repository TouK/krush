package pl.touk.exposed.converter

import pl.touk.exposed.Convert
import pl.touk.exposed.Converter
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Comment(

        @Id @GeneratedValue
        val id: Long? = null,

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
        val split = dbData.split(" ")
        return Author(split[0], split[1])
    }
}

