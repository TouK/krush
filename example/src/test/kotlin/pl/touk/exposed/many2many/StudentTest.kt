package pl.touk.exposed.many2many

import org.assertj.core.api.Assertions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test

class StudentTest {

    @Before
    fun connect() {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    }

    @Test
    fun shouldHandleManyToManyWithIdWrappers() {
        transaction {
            SchemaUtils.create(StudentTable, CourseTable, StudentCoursesTable)

            // given
            val course1 = Course(name = "kotlin")
            val course2 = Course(name = "spring cloud")

            val courses = listOf(course1, course2).map { course ->
                val courseId = CourseTable.insert { it.from(course) }[CourseTable.id]
                course.copy(id = courseId)
            }

            val student = Student(name = "John Smith", courses = courses).let { student ->
                val studentId = StudentTable.insert { it.from(student) }[StudentTable.id]
                val persistedStudent = student.copy(id = studentId)
                student.courses.forEach { course ->
                    StudentCoursesTable.insert { it.from(persistedStudent, course) }
                }
                persistedStudent
            }

            // when
            val (selectedStudent) = (StudentTable leftJoin StudentCoursesTable leftJoin CourseTable).selectAll().toStudentList()

            // then
            Assertions.assertThat(selectedStudent).isEqualTo(student)
        }
    }
}
