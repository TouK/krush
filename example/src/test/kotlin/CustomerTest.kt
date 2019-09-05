package org.kotlin.test

import generated.*
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import pl.touk.exposed.Address
import pl.touk.exposed.Customer
import pl.touk.exposed.Phone

class CustomerTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldInsertAndSelectSimpleEntity() {
        transaction {
            SchemaUtils.create(CustomerTable, PhoneTable, AddressTable)

            // given
            val customer = Customer(name = "TouK", age = 13).let { customer ->
                val customerId = CustomerTable.insert { it.from(customer) }[CustomerTable.id]
                customer.copy(id = customerId)
            }

            val phone = Phone(number = "777 888 999", customer = customer).let { phone ->
                val phoneId = PhoneTable.insert { it.from(phone) }[PhoneTable.id]
                phone.copy(id = phoneId)
            }

            val address = Address(city = "Warsaw", street = "Suwak", houseNo = "12/14", apartmentNo = "206", customer = customer).let { address ->
                val addressId = AddressTable.insert { it.from(address) }[AddressTable.id]
                address.copy(id = addressId)
            }

            // then
            val customers = (CustomerTable leftJoin PhoneTable leftJoin AddressTable).selectAll().toCustomers()

            // then
            val fullCustomer = customer.copy(addresses = listOf(address), phones = listOf(phone))
            assertThat(customers).containsOnly(fullCustomer)
        }
    }
}
