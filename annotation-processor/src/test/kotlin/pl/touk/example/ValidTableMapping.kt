package pl.touk.example

import org.joda.time.DateTime
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
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

@Entity
data class OneToOneSourceEntity(

        @Id @GeneratedValue
        val id: Long,

        @OneToOne
        @JoinColumn(name = "target_id")
        val targetEntity: OneToOneTargetEntity
)

@Entity
data class OneToOneTargetEntity(

        @Id @GeneratedValue
        val id: Long,

        @OneToOne(mappedBy = "targetEntity")
        val sourceEntity: OneToOneSourceEntity
)

@Entity
data class NumericPropertyEntity(

        @Id @GeneratedValue
        val id: Long,

        val long: Long,
        val int: Int,
        val short: Short,
        val float: Float,
        val double: Double
)

@Entity
data class DatePropertyEntity(

        @Id @GeneratedValue
        val id: Long,

        val dateTime: DateTime,
        val localDateTime: LocalDateTime,
        val zonedDateTime: ZonedDateTime
)

@Entity
data class EmbeddedPropertyEntity(
        @Id @GeneratedValue
        val id: Long,

        @Embedded
        val embeddableType: EmbeddableType
)

@Embeddable
data class EmbeddableType(
        val property1: String
)
