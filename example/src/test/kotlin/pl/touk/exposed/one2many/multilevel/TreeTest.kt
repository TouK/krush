package pl.touk.exposed.one2many.multilevel

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class TreeTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleMultipleLevelMapping() {
        transaction {
            SchemaUtils.create(TreeTable, BranchTable, LeafTable)

            // given
            val tree = Tree(name = "tree 1").let { tree ->
                val treeId = TreeTable.insert { it.from(tree) }[TreeTable.id]
                tree.copy(id = treeId)
            }

            val branch1 = Branch(name = "branch 1", tree = tree).let { branch ->
                val branchId = BranchTable.insert { it.from(branch) }[BranchTable.id]
                branch.copy(id = branchId)
            }

            val branch2 = Branch(name = "branch 2", tree = tree).let { branch ->
                val branchId = BranchTable.insert { it.from(branch) }[BranchTable.id]
                branch.copy(id = branchId)
            }

            val leaf11 = Leaf(name = "leaf11", branch = branch1).let { leaf ->
                val leafId = LeafTable.insert { it.from(leaf) }[LeafTable.id]
                leaf.copy(id = leafId)
            }

            val leaf12 = Leaf(name = "leaf12", branch = branch1).let { leaf ->
                val leafId = LeafTable.insert { it.from(leaf) }[LeafTable.id]
                leaf.copy(id = leafId)
            }

            val leaf21 = Leaf(name = "leaf21", branch = branch2).let { leaf ->
                val leafId = LeafTable.insert { it.from(leaf) }[LeafTable.id]
                leaf.copy(id = leafId)
            }

            // then
            val (trees) = (TreeTable leftJoin BranchTable leftJoin LeafTable).selectAll().toTreeList()

            // then
            val fullTree= tree.copy(branches = listOf(branch1.copy(leafs = listOf(leaf11, leaf12)), branch2.copy(leafs = listOf(leaf21))))

            assertThat(trees).isEqualTo(fullTree)
        }
    }
}
