package pl.touk.exposed.many2many

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table

@Entity
@Table(name = "articles")
data class Article(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "title")
        val title: String,

        @ManyToMany
        @JoinTable(name = "article_tags")
        val tags: List<Tag> = emptyList()
)

@Entity
@Table(name = "tags")
data class Tag(
        @Id @GeneratedValue
        val id: Long? = null,

        @Column(name = "name")
        val name: String
)
