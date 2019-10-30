package pl.touk.exposed.one2one.multilevel

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@Entity
data class Tree(

        @Id @GeneratedValue
        val id: Long? = null,

        val name: String,

        @OneToOne(mappedBy = "tree")
        val branch: Branch? = null
)

@Entity
data class Branch(

        @Id
        @GeneratedValue
        val id: Long? = null,

        val name: String,

        @OneToOne
        @JoinColumn(name = "tree_id")
        val tree: Tree? = null,

        @OneToOne(mappedBy = "branch")
        val leaf: Leaf? = null
)

@Entity
data class Leaf(

        @Id @GeneratedValue
        val id: Long? = null,

        val name: String,

        @OneToOne
        @JoinColumn(name = "branch_id")
        val branch: Branch? = null
)
