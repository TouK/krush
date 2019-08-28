package org.kotlin.test

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.exposed.Customer
import generated.CustomerTable
import generated.from
import generated.toCustomer

class CustomerTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldInsertAndSelectSimpleEntity() {
        transaction {
            SchemaUtils.create(CustomerTable)

            // given
            val customer = Customer(name = "TouK", age = 13)

            // when
            val id = CustomerTable.insert { it.from(customer) }[CustomerTable.id]
            val customers: List<Customer> = CustomerTable.selectAll().map { it.toCustomer() }

            // then
            assertThat(customers)
                    .containsOnly(customer.copy(id = id))
        }
    }
}
