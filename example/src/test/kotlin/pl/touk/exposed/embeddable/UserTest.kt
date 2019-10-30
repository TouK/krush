package pl.touk.exposed.embeddable

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class UserTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleEmbeddedTypes() {
        transaction {
            SchemaUtils.create(UserTable)

            // given
            val contactAddress = Address.ContactAddress(city = "Warsaw", street = "Aleja Bohaterów Września", houseNumber = 9)

            val user = User(contactAddress = contactAddress).let { user ->
                val userId = UserTable.insert { it.from(user) }[UserTable.id]
                user.copy(id = userId)
            }

            // when
            val selectedUsers = UserTable.selectAll().toUserList()

            // then
            Assertions.assertThat(selectedUsers).containsOnly(user)
        }
    }
}
