package pl.touk.krush.one2many.uni

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class TreeTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleMultipleLevelMapping() {
        transaction {
            SchemaUtils.create(TreeTable, BranchTable, LeafTable)

            // given
            val tree = Tree(name = "tree 1").let(TreeTable::insert)

            val branch1 = Branch(name = "branch 1").let { branch -> BranchTable.insert(branch, tree) }
            val branch2 = Branch(name = "branch 2").let { branch -> BranchTable.insert(branch, tree) }

            val leaf11 = LeafTable.insert(Leaf(name = "leaf11"), branch1)
            val leaf12 = LeafTable.insert(Leaf(name = "leaf12"), branch1)
            val leaf21 = LeafTable.insert(Leaf(name = "leaf21"), branch2)

            // then
            val (trees) = (TreeTable leftJoin BranchTable leftJoin LeafTable)
                    .select { BranchTable.treeId eq tree.id }
                    .toTreeList()

            // then
            val fullTree= tree.copy(branches = listOf(branch1.copy(leafs = listOf(leaf11, leaf12)), branch2.copy(leafs = listOf(leaf21))))

            assertThat(trees).isEqualTo(fullTree)
        }
    }
}
