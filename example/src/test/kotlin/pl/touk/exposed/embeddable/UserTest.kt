package pl.touk.exposed.embeddable

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.exposed.embeddable.Address.ContactAddress
import pl.touk.exposed.embeddable.Address.InvoiceAddress

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
            val contactAddress = ContactAddress(city = "Warsaw", street = "Aleja Bohaterów Września", houseNumber = 9)
            val invoiceAddress = InvoiceAddress(city = "Warsaw", street = "Aleje Jerozolimskie", houseNumber = 0)

            val user = User(contactAddress = contactAddress, invoiceAddress = invoiceAddress).let { user ->
                val userId = UserTable.insert { it.from(user) }[UserTable.id]
                user.copy(id = userId)
            }

            // when
            val selectedUsers = UserTable
                    .select { (UserTable.contactAddressCity.lowerCase() eq "warsaw") and (UserTable.contactAddressHouseNumber greaterEq 9) }
                    .toUserList()

            // then
            Assertions.assertThat(selectedUsers).containsOnly(user)
        }
    }
}
