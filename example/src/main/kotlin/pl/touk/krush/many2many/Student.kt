package pl.touk.krush.many2many

import pl.touk.krush.common.IdNotPersistedDelegate
import pl.touk.krush.common.RefId
import javax.persistence.*

@Entity
@Table(name = "students")
data class Student(

    @Id @GeneratedValue
    @Convert(converter = StudentIdConverter::class)
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
    @Convert(converter = CourseIdConverter::class)
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

class StudentIdConverter : AttributeConverter<StudentId, Long> {

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

class CourseIdConverter : AttributeConverter<CourseId, Long> {

    override fun convertToDatabaseColumn(attribute: CourseId): Long {
        return attribute.value
    }

    override fun convertToEntityAttribute(dbData: Long): CourseId {
        return CourseId.Persisted(dbData)
    }
}
