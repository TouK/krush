package pl.touk.krush.many2one

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class CustomerTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleManyToOneTypes() {
        transaction {
            SchemaUtils.create(CustomerTable)
            SchemaUtils.create(ContactPersonTable)

            // given
            val contactPerson = ContactPerson(firstName = "Willy", lastName = "Wonka")
                .let(ContactPersonTable::insert)
            val customer = Customer(name = "Chocolate Factory, Inc.", contactPerson = contactPerson)
                .let(CustomerTable::insert)

            // when
            val customers = (CustomerTable leftJoin ContactPersonTable).selectAll().toCustomerList()

            // then
            assertThat(customers).containsExactly(customer)
        }
    }

}
