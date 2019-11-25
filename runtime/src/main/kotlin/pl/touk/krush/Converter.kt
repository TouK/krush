package pl.touk.krush

interface Converter<X, Y> {

    fun convertToDatabaseColumn(attribute: X): Y

    fun convertToEntityAttribute(dbData: Y): X
}
