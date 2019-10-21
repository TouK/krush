package pl.touk.example

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.print.attribute.IntegerSyntax

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
data class OneToOneSourceEntity (

        @Id @GeneratedValue
        val id: Long,

        @OneToOne
        @JoinColumn(name = "target_id")
        val targetEntity: OneToOneTargetEntity
)

@Entity
data class OneToOneTargetEntity (

        @Id @GeneratedValue
        val id: Long,

        @OneToOne(mappedBy = "targetEntity")
        val sourceEntity: OneToOneSourceEntity
)

@Entity
data class NumericPropertyEntity (
        @Id @GeneratedValue
        val id: Long,

        val prop1: Long,
        val prop2: Int,
        val prop3: Short,
        val prop4: Float,
        val prop5: Double
)