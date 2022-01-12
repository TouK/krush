package pl.touk.krush.realreferences

import javax.persistence.*

@Entity
@Table(name = "category")
data class Category(
        @Id
        @GeneratedValue
        val id: Long? = null,

        @Column
        val name: String,

        @OneToOne
        @JoinColumn(name = "parent_id")
        val parent: Category?,

        @OneToMany(mappedBy = "parent")
        val children: List<Category> = emptyList()
)
