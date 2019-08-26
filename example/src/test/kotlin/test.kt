package org.kotlin.test

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import test.generated.CustomerTable

class AnnotationTest {
    @Test fun testSimple() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(CustomerTable)
            CustomerTable.selectAll()
        }
    }
}
