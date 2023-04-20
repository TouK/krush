package pl.touk.krush.distincton

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class TenantVersionedJsonContentTest : BaseDatabaseTest() {

    @Test
    fun shouldGetNewestContents() {
        val contentId1 = ContentId.generate()
        val contentId2 = ContentId.generate()

        val tenantId1 = TenantId.generate()
        val tenantId2 = TenantId.generate()

        transaction {
            TenantVersionedJsonContent.insert(TenantJsonContent(contentId1, tenantId1, "\"{'field1': 'value1'}\""))
            TenantVersionedJsonContent.insert(TenantJsonContent(contentId1, tenantId1, "\"{'field1': 'value2', 'field2': 10}\""))
            TenantVersionedJsonContent.insert(TenantJsonContent(contentId2, tenantId1, "\"{'field1': 'value3', 'field3': 5}\""))
            TenantVersionedJsonContent.insert(TenantJsonContent(contentId1, tenantId2, "\"{'field1': 'value4', 'field3': 7}\""))
            TenantVersionedJsonContent.insert(TenantJsonContent(contentId1, tenantId2, "\"{'field1': 'value5', 'field3': 7}\""))
        }

        transaction {
            assertThat(TenantVersionedJsonContent.getNewestContents()).containsExactlyInAnyOrder(
                TenantJsonContent(contentId1, tenantId1, "\"{'field1': 'value2', 'field2': 10}\""),
                TenantJsonContent(contentId2, tenantId1, "\"{'field1': 'value3', 'field3': 5}\""),
                TenantJsonContent(contentId1, tenantId2, "\"{'field1': 'value5', 'field3': 7}\""),
            )
        }
    }
}