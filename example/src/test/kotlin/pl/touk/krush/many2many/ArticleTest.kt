package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class ArticleTest : BaseDatabaseTest() {

    @AfterEach
    internal fun tearDown() {
        transaction {
            ArticleTagsTable.deleteAll()
            TagTable.deleteAll()
            ArticleTable.deleteAll()
        }
    }

    @Test
    fun shouldHandleManyToMany() {
        transaction {
            SchemaUtils.create(ArticleTable, TagTable, ArticleTagsTable)

            // given
            val tagJvm = Tag(name = "jvm")
            val tagSpring = Tag(name = "spring")
            val tagTutorial = Tag(name = "tutorial")

            val tags = listOf(tagJvm, tagSpring, tagTutorial).map(TagTable::insert)
            val article = ArticleTable.insert(Article(title = "Spring for dummies", tags = tags))

            // when
            val selectedArticles = (ArticleTable leftJoin ArticleTagsTable leftJoin TagTable)
                    .select { ArticleTable.title like "Spring%" }
                    .toArticleList()

            // then
            assertThat(selectedArticles).containsOnly(article)
        }
    }

    @Test
    fun shouldHandleManyToManyWithMissingColumns() {
        transaction {
            SchemaUtils.create(ArticleTable, TagTable, ArticleTagsTable)

            // given
            val tagJvm = Tag(name = "jvm")
            val tagSpring = Tag(name = "spring")
            val tagTutorial = Tag(name = "tutorial")

            val tags = listOf(tagJvm, tagSpring, tagTutorial).map(TagTable::insert)
            val article = ArticleTable.insert(Article(title = "Spring for dummies", tags = tags))

            // when
            val selectedArticles = ArticleTable
                .selectAll()
                .toArticleList()

            // then
            assertThat(selectedArticles).containsExactly(article.copy(tags = emptyList()))
        }
    }
}
