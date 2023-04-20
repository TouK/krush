package pl.touk.krush.distincton

import org.jetbrains.exposed.sql.Table
import pl.touk.krush.integerWrapper
import pl.touk.krush.uuidWrapper
import java.util.UUID

@JvmInline
value class ContentId(val raw: UUID) {
    companion object {
        fun generate(): ContentId = ContentId(UUID.randomUUID())
    }
}

@JvmInline
value class TenantId(val raw: UUID) {
    companion object {
        fun generate(): TenantId = TenantId(UUID.randomUUID())
    }
}

@JvmInline
value class Version(val raw: Int) : Comparable<Version> {
    override fun compareTo(other: Version): Int = raw.compareTo(other.raw)
    fun increment(): Version = Version(raw + 1)

    companion object {
        val initial: Version = Version(1)
    }
}

fun Table.contentId(name: String) = uuidWrapper(name, ::ContentId) { it.raw }
fun Table.tenantId(name: String) = uuidWrapper(name, ::TenantId) { it.raw }
fun Table.version(name: String) = integerWrapper(name, ::Version) { it.raw }
