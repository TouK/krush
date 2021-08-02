@file:Suppress("UNCHECKED_CAST")

package pl.touk.krush.realreferences

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Alias
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.RowWrapper
import pl.touk.krush.base.BaseDatabaseTest
import kotlin.reflect.KClass

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

            val fullCategoryMappping =
                CategoryTable.join(parentAlias, JoinType.LEFT, CategoryTable.parentId, parentAlias[CategoryTable.id])
                    .selectAll()
                    .toCategoryList(parentAlias)

            assertThat(fullCategoryMappping)
                .containsExactlyInAnyOrder(parent, child1, child2)
        }
    }

}

fun ResultRow.toCategory(parentAlias: Alias<CategoryTable>?): Category = RowWrapper(this).toCategory(parentAlias)

fun Iterable<ResultRow>.toCategoryList(parentAlias: Alias<CategoryTable>?): List<Category> =
    this.toCategoryMap(parentAlias).values.toList()

fun Iterable<ResultRow>.toCategoryMap(parentAlias: Alias<CategoryTable>?): Map<Long, Category> {
    val entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()
    val selfReferenceRequests: MutableMap<KClass<*>, MutableMap<Any, MutableSet<Any>>> =
        mutableMapOf()
    this.forEach { row ->
        // 	Create this entity or expand on the sub-entity lists contained within
        val rowWrapper = RowWrapper(row, entityStore, selfReferenceRequests)
        val currentCategory = rowWrapper.toCategory(parentAlias)
        rowWrapper.addSubEntitiesToCategory(currentCategory)
    }
    selfReferenceRequests.forEach { (clazz, unsatisfiedMap) ->
        when(clazz) {
            Category::class -> unsatisfiedMap.forEach { (subjectCategoryId, referencingCategoryIds) ->
                val subjectCategory = entityStore[Category::class]?.get(subjectCategoryId) as? Category
                if (subjectCategory != null) {
                    referencingCategoryIds.forEach { referencingCategoryId ->
                        val referencingCategory = entityStore[Category::class]?.get(referencingCategoryId) as?
                                Category
                        (referencingCategory?.children as? MutableList<Category>)?.add(subjectCategory)
                    }
                }
            }
        }
    }
    return (entityStore[Category::class] ?: emptyMap()) as Map<Long, Category>
}

fun RowWrapper.addSubEntitiesToCategory(category: Category?) {
    if (category == null) return
    // Add sub-elements contained in this row to parent
    addSubEntitiesToCategory(category.parent)
    val childrenId = row.getOrNull(CategoryTable.id)
    if (childrenId != null && !containsEntity(Category::class, childrenId)) {
        val childrenAttr = category.children as MutableList<Category>
        val childrenAttrLastElement = childrenAttr.lastOrNull()
        // Prevent stack overflow when mapping bi-directional relations
        withoutEntity(Category::class, category.id) {
            if (childrenId != childrenAttrLastElement?.id) {
                // If the sub-entity is new, create a new object for it
                val newCategory = toCategory(null)
                addSubEntitiesToCategory(newCategory)
                childrenAttr.add(newCategory)
            } else {
                // 	If we already have an entity with this ID, check if there's a new sub-sub-entity in it
                addSubEntitiesToCategory(childrenAttrLastElement)
            }
        }
    }
}

fun RowWrapper.toCategory(parentAlias: Alias<CategoryTable>?): Category {
    val id = row[CategoryTable.id]
    val cacheMap = entityStore.getOrPut(Category::class) { mutableMapOf() }
    return cacheMap.getOrPut(id) {
        Category(
            id = id,
            name = row[CategoryTable.name],
            parent = row[CategoryTable.parentId]?.let { parentAlias?.let { this.toCategory(parentAlias, null) } },
            children = mutableListOf()
        )
    } as Category
}

fun RowWrapper.toCategory(alias: Alias<CategoryTable>, parentAlias: Alias<CategoryTable>?): Category = Category(
    id = row[alias[CategoryTable.id]],
    name = row[alias[CategoryTable.name]],
    parent = row[CategoryTable.parentId]?.let { parentAlias?.let { this.toCategory(parentAlias, null) } },
    children = mutableListOf()
)
