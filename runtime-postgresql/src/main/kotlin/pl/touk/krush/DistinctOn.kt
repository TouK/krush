package pl.touk.krush

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Slice
import org.jetbrains.exposed.sql.Table

/**
 * There is no postgresql distinct on implemented yet (https://github.com/JetBrains/Exposed/issues/500)
 *
 * Current implementation assumes that:
 * - `distinct on` can be used on single or multiple number of columns
 * - all columns of `distinct on` clause are also columns present in the query
 * - each column of `distinct on` clause can be referenced normally in `order by` etc.
 *
 * To allow above:
 * - custom slice is introduced because it is less intrusive way to extend exposed with `distinct on` clause
 * - after `distinct on (...)` there will be `TRUE` part added
 */

fun <T> Column<T>.distinctOn(): DistinctOnColumns<T> = DistinctOnColumns(this)

fun <T> distinctOn(expr: ExpressionWithColumnType<T>, vararg others: Expression<*>) = DistinctOnColumns(expr, *others)

fun Table.slice(distinctOnColumns: DistinctOnColumns<*>, vararg columns: Expression<*>): FieldSet {
    val distinctOn = distinctOnColumns.toDistinctOn()
    return Slice(this, listOf(distinctOn) + distinctOn.fieldset() + columns)
}

class DistinctOnColumns<T>(
    private val expr: ExpressionWithColumnType<T>,
    private vararg val others: Expression<*>,
) {
    fun toDistinctOn() = DistinctOn(expr, *others)
}

class DistinctOn<T>(
    private val expr: ExpressionWithColumnType<T>,
    private vararg val others: Expression<*>,
) : Function<T>(expr.columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("DISTINCT ON(")
        (fieldset()).appendTo(separator = ", ") { +it }
        append(") TRUE")
    }

    fun fieldset() = listOf(expr) + others
}
