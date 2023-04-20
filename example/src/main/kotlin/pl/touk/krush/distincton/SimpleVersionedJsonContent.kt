package pl.touk.krush.distincton

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import pl.touk.krush.distinctOn
import pl.touk.krush.jsonb
import pl.touk.krush.slice

object SimpleVersionedJsonContent : Table("simple_versioned_json_content") {
    val id = contentId("id")
    val version = version("version")
    val content = jsonb("content")

    fun insert(id: ContentId, content: String) {
        SimpleVersionedJsonContent.insert {
            it[SimpleVersionedJsonContent.id] = id
            it[SimpleVersionedJsonContent.content] = content
            it[SimpleVersionedJsonContent.version] = findNewestVersion(id)?.increment() ?: Version.initial
        }
    }

    fun findNewestContent(id: ContentId): String? =
        SimpleVersionedJsonContent
            .slice(SimpleVersionedJsonContent.id.distinctOn(), SimpleVersionedJsonContent.content)
            .select { SimpleVersionedJsonContent.id eq id }
            .orderBy(SimpleVersionedJsonContent.id to SortOrder.ASC, SimpleVersionedJsonContent.version to SortOrder.DESC)
            .map { it[SimpleVersionedJsonContent.content] }
            .firstOrNull()

    fun findOldestContent(id: ContentId): String? =
        SimpleVersionedJsonContent
            .slice(SimpleVersionedJsonContent.id.distinctOn(), SimpleVersionedJsonContent.content)
            .select { SimpleVersionedJsonContent.id eq id }
            .orderBy(SimpleVersionedJsonContent.id to SortOrder.ASC, SimpleVersionedJsonContent.version to SortOrder.ASC)
            .map { it[SimpleVersionedJsonContent.content] }
            .firstOrNull()

    private fun findNewestVersion(id: ContentId): Version? =
        SimpleVersionedJsonContent
            .slice(SimpleVersionedJsonContent.version.max())
            .select { SimpleVersionedJsonContent.id eq id }
            .map { it[SimpleVersionedJsonContent.version.max()] }
            .firstOrNull()
}