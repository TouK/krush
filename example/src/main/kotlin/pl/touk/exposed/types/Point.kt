package pl.touk.exposed.types

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Point(

        @Id @GeneratedValue
        val id: Long? = null,

        val x1: Long,

        val y1: Int,

        val z1: Short,

        val x2: Float,

        val y2: Double
)