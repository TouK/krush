package pl.touk.krush.multipackage.b

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "test_b")
data class TestB(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column val text: String
)
