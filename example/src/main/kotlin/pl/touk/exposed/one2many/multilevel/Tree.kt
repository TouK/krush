package pl.touk.exposed.one2many.multilevel

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
data class Tree(

        @Id @GeneratedValue
        val id: Long? = null,

        val name: String,

        @OneToMany(mappedBy = "tree")
        val branches: List<Branch> = emptyList()
)

@Entity
data class Branch(

        @Id
        @GeneratedValue
        val id: Long? = null,

        val name: String,

        @ManyToOne
        @JoinColumn(name = "tree_id")
        val tree: Tree? = null,

        @OneToMany(mappedBy = "branch")
        val leafs: List<Leaf> = emptyList()
)

@Entity
data class Leaf(

        @Id @GeneratedValue
        val id: Long? = null,

        val name: String,

        @ManyToOne
        @JoinColumn(name = "branch_id")
        val branch: Branch? = null
)
