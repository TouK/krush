package pl.touk.exposed.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class ArticleTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleManyToMany() {
        transaction {
            SchemaUtils.create(ArticleTable, TagTable, ArticleTagsTable)

            // given
            val tag1 = Tag(name = "jvm")
            val tag2 = Tag(name = "spring")

            val tags = listOf(tag1, tag2).map { tag ->
                val tagId = TagTable.insert { it.from(tag) }[TagTable.id]
                tag.copy(id = tagId)
            }

            val article = Article(title = "Spring for dummies", tags = tags).let { article ->
                val articleId = ArticleTable.insert { it.from(article) }[ArticleTable.id]
                val persistedArticle = article.copy(id = articleId)
                article.tags.forEach { tag ->
                    ArticleTagsTable.insert { it.from(persistedArticle, tag) }
                }
                persistedArticle
            }

            // when
            val (selectedArticle) = (ArticleTable leftJoin ArticleTagsTable leftJoin TagTable).selectAll().toArticleList()

            // then
            assertThat(selectedArticle).isEqualTo(article)
        }
    }
}
