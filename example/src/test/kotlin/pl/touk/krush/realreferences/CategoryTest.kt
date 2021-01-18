package pl.touk.krush.realreferences

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

            CategoryTable.join(parentAlias, JoinType.LEFT, CategoryTable.parent, parentAlias[CategoryTable.id])
                .selectAll()
                .forEach { row ->
                    val childName = row[CategoryTable.name]
                    val parentName = row[parentAlias[CategoryTable.name]]
                    println("$childName -> $parentName")
                }
        }
    }

}
