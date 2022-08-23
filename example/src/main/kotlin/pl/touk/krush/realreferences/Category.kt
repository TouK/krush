package pl.touk.krush.realreferences

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "category")
data class Category(
        @Id
        val uuid: UUID = UUID.randomUUID(),

        @Column
        val name: String,

        @ManyToOne
        val parent: Category?,

        @OneToMany(mappedBy = "parent")
        val children: List<Category> = emptyList()
)
