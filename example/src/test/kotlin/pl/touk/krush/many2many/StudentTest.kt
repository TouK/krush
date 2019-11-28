package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class StudentTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleManyToManyWithIdWrappers() {
        transaction {
            SchemaUtils.create(StudentTable, CourseTable, StudentCoursesTable)

            // given
            val course1 = Course(name = "kotlin")
            val course2 = Course(name = "spring cloud")

            val courses = listOf(course1, course2).map(CourseTable::insert)

            val student = StudentTable.insert(Student(name = "John Smith", courses = courses))

            // when
            val (selectedStudent) = (StudentTable leftJoin StudentCoursesTable leftJoin CourseTable)
                    .select { CourseTable.id inList courses.map { it.id } }
                    .toStudentList()

            // then
            assertThat(selectedStudent).isEqualTo(student)
        }
    }
}
