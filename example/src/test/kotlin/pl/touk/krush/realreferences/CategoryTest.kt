@file:Suppress("UNCHECKED_CAST")

package pl.touk.krush.realreferences

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class CategoryTest: BaseDatabaseTest() {

    @Test
    fun shouldCreateCategories() {
        transaction {
            SchemaUtils.create(CategoryTable)

            val parentAlias = CategoryTable.alias("parent")

            val parent = CategoryTable.insert(Category(name = "parent", parent = null))
            val child1 = CategoryTable.insert(Category(name = "child1", parent = parent))
            val child2 = CategoryTable.insert(Category(name = "child2", parent = parent))

            val categories =
                CategoryTable.join(parentAlias, JoinType.LEFT, CategoryTable.parentId, parentAlias[CategoryTable.id])
                .selectAll()
                .map { it.toCategory(parentAlias) }

            assertThat(categories)
                .containsExactlyInAnyOrder(parent, child1, child2)

            val fullCategoryMapping =
                CategoryTable.join(parentAlias, JoinType.LEFT, CategoryTable.parentId, parentAlias[CategoryTable.id])
                    .selectAll()
                    .toCategoryList(parentAlias)

            assertThat(fullCategoryMapping)
                .containsExactlyInAnyOrder(parent, child1, child2)
        }
    }

}
