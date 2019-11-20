package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
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

            val tags = listOf(tag1, tag2).map(TagTable::insert)
            val article = ArticleTable.insert(Article(title = "Spring for dummies", tags = tags))

            // when
            val (selectedArticle) = (ArticleTable leftJoin ArticleTagsTable leftJoin TagTable)
                    .select { TagTable.name inList listOf("jvm", "spring") }
                    .toArticleList()

            // then
            assertThat(selectedArticle).isEqualTo(article)
        }
    }
}
