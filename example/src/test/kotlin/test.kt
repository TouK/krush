package org.kotlin.test

import org.jetbrains.exposed.sql.selectAll
import org.junit.Test
import test.generated.CustomerTable

class AnnotationTest {
    @Test fun testSimple() {
        CustomerTable.selectAll()
    }
}
