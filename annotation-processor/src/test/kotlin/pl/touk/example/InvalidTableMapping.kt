package pl.touk.example

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class InvalidClassEntity(

        @Id @GeneratedValue
        val id: Long?,

        val prop: String
)

@Entity
data class IdNotPresentEntity(
        val prop: String
)

@Entity
data class IdTypeUnsupportedEntity(
        @Id @GeneratedValue
        val id: Float?
)

@Entity
data class PropertyTypeUnsupportedEntity(
        @Id @GeneratedValue
        val id: Long?,

        val prop: Pair<String, String>
)
