package pl.touk.example

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
data class DefaultPropertyNameEntity(

        @Id @GeneratedValue
        val id: Long,

        val prop1: String,

        @Column
        val prop2: String
)

@Entity
@Table(name = "entity")
data class CustomPropertyNameEntity(

        @Id @GeneratedValue
        @Column(name = "test_id")
        val id: Long,

        @Column(name = "prop1_custom")
        val prop1: String
)

@Entity
data class NullablePropertyEntity(

        @Id @GeneratedValue
        val id: Long,

        val prop1: String?
)