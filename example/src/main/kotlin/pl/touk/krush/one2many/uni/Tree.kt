package pl.touk.krush.one2many.uni

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

@Entity
data class Tree(

    @Id @GeneratedValue
    val id: Long? = null,

    val name: String,

    @OneToMany
    @JoinColumn(name = "my_tree_id")
    val branches: List<Branch> = emptyList()
)

@Entity
data class Branch(

    @Id
    @GeneratedValue
    val id: Long? = null,

    val name: String,

    @OneToMany
    @JoinColumn(name = "branch_id")
    val leafs: List<Leaf> = emptyList()
)

@Entity
data class Leaf(

    @Id @GeneratedValue
    val id: Long? = null,

    val name: String
)
