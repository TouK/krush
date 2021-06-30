package pl.touk.krush.realreferences

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
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

            val fullCategories =
                CategoryTable.join(parentAlias, JoinType.LEFT, CategoryTable.parentId, parentAlias[CategoryTable.id])
                    .selectAll()
                    .toCategoryList(parentAlias)

            println(fullCategories)
        }
    }

}

fun Iterable<ResultRow>.toCategoryList(parentAlias: Alias<CategoryTable>?): List<Category> =
    this.toCategoryMap(parentAlias).values.toList()

fun Iterable<ResultRow>.toCategoryMap(parentAlias: Alias<CategoryTable>?): MutableMap<Long, Category> {
    val roots = mutableMapOf<Long, Category>()
    val category_parent = mutableMapOf<Long, Category>()
    val category_children = mutableMapOf<Long, MutableSet<Category>>()
    val children_map = emptyMap<kotlin.Long, Category>()
    this.forEach { resultRow ->
        val categoryId = resultRow.getOrNull(CategoryTable.id)
        if(categoryId == null) return@forEach

        val category = roots[categoryId] ?: resultRow.toCategory(parentAlias)
        roots[categoryId] = category

        resultRow[CategoryTable.parentId]?.let { parentId ->
            val parent = resultRow.toCategory(parentAlias)
            category_parent[categoryId] = parent
            category_children.getOrPut(parentId, ::mutableSetOf).add(category)
        }
    }

    return roots.mapValues { (_, category) ->
        category.copy(
            parent = category_parent[category.id],
            children = category_children[category.id]?.toList() ?: emptyList()
        )
    }.toMutableMap()

}

fun ResultRow.toCategory(parentAlias: Alias<CategoryTable>?): Category = Category(
    id = this[CategoryTable.id],
    name = this[CategoryTable.name],
    parent = this[CategoryTable.parentId]?.let { parentAlias?.let { this.toCategory(parentAlias, null) } },
    children = mutableListOf()
)

fun ResultRow.toCategory(alias: Alias<CategoryTable>, parentAlias: Alias<CategoryTable>?): Category = Category(
    id = this[alias[CategoryTable.id]],
    name = this[alias[CategoryTable.name]],
    parent = this[CategoryTable.parentId]?.let { parentAlias?.let { this.toCategory(parentAlias, null) } },
    children = mutableListOf()
)
