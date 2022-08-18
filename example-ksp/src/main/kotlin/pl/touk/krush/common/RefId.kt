package pl.touk.krush.common

import kotlin.reflect.KProperty

abstract class RefId<T : Comparable<T>> : Comparable<RefId<T>> {
    abstract val value: T

    override fun compareTo(other: RefId<T>) = value.compareTo(other.value)
}

class IdNotPersistedDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Nothing = throw IllegalStateException("Id not persisted yet")
}
