package pl.touk.exposed.multipackage.b

import javax.persistence.*

@Entity
@Table(name = "test_b")
data class TestB(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column val text: String
)
