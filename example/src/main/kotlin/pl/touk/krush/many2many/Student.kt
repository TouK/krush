package pl.touk.krush.many2many

import pl.touk.krush.Convert
import pl.touk.krush.Converter
import pl.touk.krush.converter.IdNotPersistedDelegate
import pl.touk.krush.converter.RefId
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table
import kotlin.reflect.KProperty

@Entity
@Table(name = "students")
data class Student(

        @Id @GeneratedValue
        @Convert(value = StudentIdConverter::class)
        val id: StudentId = StudentId.New,

        @Column(name = "name")
        val name: String,

        @ManyToMany
        @JoinTable(name = "student_courses")
        val courses: List<Course> = emptyList()
)

@Entity
@Table(name = "courses")
data class Course(

        @Id @GeneratedValue
        @Convert(value = CourseIdConverter::class)
        val id: CourseId = CourseId.New,

        @Column(name = "name")
        val name: String
)

sealed class StudentId : RefId<Long>() {

        object New : StudentId() {
                override val value: Long by IdNotPersistedDelegate<Long>()
        }

        data class Persisted(override val value: Long) : StudentId() {
                override fun toString() = "StudentId(value=$value)"
        }
}

class StudentIdConverter : Converter<StudentId, Long> {

        override fun convertToDatabaseColumn(attribute: StudentId): Long {
                return attribute.value
        }

        override fun convertToEntityAttribute(dbData: Long): StudentId {
                return StudentId.Persisted(dbData)
        }
}

sealed class CourseId : RefId<Long>() {

    object New : CourseId() {
        override val value: Long by IdNotPersistedDelegate<Long>()
    }

    data class Persisted(override val value: Long) : CourseId() {
        override fun toString() = "CourseId(value=$value)"
    }
}

class CourseIdConverter : Converter<CourseId, Long> {

        override fun convertToDatabaseColumn(attribute: CourseId): Long {
                return attribute.value
        }

        override fun convertToEntityAttribute(dbData: Long): CourseId {
                return CourseId.Persisted(dbData)
        }
}


abstract class RefId<T : Comparable<T>> : Comparable<RefId<T>> {
        abstract val value: T

        override fun compareTo(other: RefId<T>) = value.compareTo(other.value)
}

class IdNotPersistedDelegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Nothing = throw IllegalStateException("Id not persisted yet")
}
