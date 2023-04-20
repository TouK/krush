package pl.touk.krush.distincton

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class SimpleVersionedJsonContentTest : BaseDatabaseTest() {

    private val contentId1 = ContentId.generate()
    private val contentId2 = ContentId.generate()

    @BeforeAll
    fun insertTestData() {
        transaction {
            SimpleVersionedJsonContent.insert(contentId1, "\"{'field1': 'value1'}\"")
            SimpleVersionedJsonContent.insert(contentId1, "\"{'field1': 'value2', 'field2': 10}\"")
            SimpleVersionedJsonContent.insert(contentId2, "\"{'field1': 'value3', 'field3': 5}\"")
            SimpleVersionedJsonContent.insert(contentId2, "\"{'field1': 'value4', 'field4': 7}\"")
        }
    }

    @Test
    fun shouldGetNewestContent() {
        transaction {
            assertThat(SimpleVersionedJsonContent.findNewestContent(contentId1)).isEqualTo("\"{'field1': 'value2', 'field2': 10}\"")
            assertThat(SimpleVersionedJsonContent.findNewestContent(contentId2)).isEqualTo("\"{'field1': 'value4', 'field4': 7}\"")
            assertThat(SimpleVersionedJsonContent.findNewestContent(ContentId.generate())).isNull()
        }
    }

    @Test
    fun shouldGetOldestContent() {
        transaction {
            assertThat(SimpleVersionedJsonContent.findOldestContent(contentId1)).isEqualTo("\"{'field1': 'value1'}\"")
            assertThat(SimpleVersionedJsonContent.findOldestContent(contentId2)).isEqualTo("\"{'field1': 'value3', 'field3': 5}\"")
            assertThat(SimpleVersionedJsonContent.findOldestContent(ContentId.generate())).isNull()
        }
    }
}