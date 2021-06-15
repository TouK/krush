package pl.touk.krush

import org.jetbrains.exposed.sql.ResultRow
import kotlin.reflect.KClass

data class RowWrapper(
    var row: ResultRow,
    val entityStore: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf(),
    val selfReferenceRequests: MutableMap<KClass<*>, MutableMap<Any, MutableSet<Any>>> = mutableMapOf()
)
