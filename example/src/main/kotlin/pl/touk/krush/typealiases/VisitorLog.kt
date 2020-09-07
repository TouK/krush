package pl.touk.krush.typealiases

import javax.persistence.*

typealias VisitorList = List<String>
typealias PlainString = String

@Entity
data class VisitorLog(
        @Id @GeneratedValue
        val id: Long? = null,
        @Convert(converter = VisitorListConverter::class)
        val visitors: VisitorList,
        val guard: PlainString
)

@Converter
class VisitorListConverter : AttributeConverter<VisitorList, String> {
    override fun convertToDatabaseColumn(attribute: VisitorList?): String {
        return attribute?.joinToString(",") ?: ""
    }

    override fun convertToEntityAttribute(dbData: String?): VisitorList {
        return dbData?.split(",") ?: emptyList()
    }
}