package pl.touk.exposed.multipackage

import pl.touk.exposed.multipackage.b.TestB
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "test_a")
data class TestA(
        @Id @GeneratedValue
        val id: Long? = null,

        @OneToMany
        @JoinColumn(name = "testA_id")
        val b: List<TestB> = emptyList()
)
