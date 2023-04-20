package pl.touk.krush.distincton

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import pl.touk.krush.distinctOn
import pl.touk.krush.jsonb
import pl.touk.krush.slice

data class TenantJsonContent(val id: ContentId, val tenantId: TenantId, val content: String)

object TenantVersionedJsonContent : Table("tenant_versioned_json_content") {
    val id = contentId("id")
    val tenantId = tenantId("tenant_id")
    val version = version("version")
    val content = jsonb("content")

    fun insert(tenantJsonContent: TenantJsonContent) {
        val currentVersion = findNewestVersion(tenantJsonContent.id, tenantJsonContent.tenantId)
        TenantVersionedJsonContent.insert {
            it[id] = tenantJsonContent.id
            it[tenantId] = tenantJsonContent.tenantId
            it[content] = tenantJsonContent.content
            it[version] = currentVersion?.increment() ?: Version.initial
        }
    }

    fun getNewestContents(): List<TenantJsonContent> =
        TenantVersionedJsonContent
            .slice(distinctOn(id, tenantId), content)
            .selectAll()
            .orderBy(id to SortOrder.ASC, tenantId to SortOrder.ASC, version to SortOrder.DESC)
            .map {
                TenantJsonContent(
                    id = it[id],
                    tenantId = it[tenantId],
                    content = it[content],
                )
            }

    private fun findNewestVersion(id: ContentId, tenantId: TenantId): Version? =
        TenantVersionedJsonContent
            .slice(version.max())
            .select { TenantVersionedJsonContent.id eq id and (TenantVersionedJsonContent.tenantId eq tenantId) }
            .map { it[version.max()] }
            .firstOrNull()
}