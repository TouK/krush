package pl.touk.krush.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KrushKSPTest {

    @Test
    fun `should build graph`() {
        val source = SourceFile.kotlin(
            "Book.kt", """
                package pl.touk.krush

                import javax.persistence.*
                import java.time.LocalDate
                
                @Entity
                @Table(name = "books")
                data class Book(
                    @Id @GeneratedValue
                    val id: Int? = null,
                    @Column(name = "ISBN")
                    val isbn: String,
                    val author: String,
                    val title: String,
                    val publishDate: LocalDate
                )
            """
        )

        assertCompiles(source)
    }

    @Test
    fun `should find converter`() {
        val source = SourceFile.kotlin(
            "Thread.kt", """
                package pl.touk.krush.example

                import javax.persistence.*
                
                data class ThreadId(val value: Long)
                
                @Entity
                data class Thread(
                    @Id @GeneratedValue
                    @Convert(converter = ThreadConverter::class)
                    val id: ThreadId = ThreadId(-1),
                
                    val name: String
                )
                
                class ThreadConverter : AttributeConverter<ThreadId, Long> {
                
                    override fun convertToDatabaseColumn(attribute: ThreadId): Long {
                        return attribute.value
                    }
                
                    override fun convertToEntityAttribute(dbData: Long): ThreadId {
                        return ThreadId(dbData)
                    }
                }

            """
        )

        assertCompiles(source)
    }

    @Test
    fun shouldProcessEnum() {
        val source = SourceFile.kotlin(
            "Message.kt", """
                package pl.touk.krush.example

                import javax.persistence.*

                enum class MessageStatus {
                    NEW, PENDING, RECEIVED
                }

                @Entity
                data class Message(
                    @Id @GeneratedValue
                    val id: Long? = null,

                    @Enumerated(EnumType.STRING)
                    val status: MessageStatus,

                    @Enumerated(EnumType.ORDINAL)
                    val previousStatus: MessageStatus
                )
            """.trimIndent()
        )

        assertCompiles(source)
    }

    @Test
    fun shouldProcessEmbeddedId() {
        val source = SourceFile.kotlin(
            "Record.kt", """
                import java.time.LocalDateTime
                import javax.persistence.Column
                import javax.persistence.Embeddable
                import javax.persistence.EmbeddedId
                import javax.persistence.Entity

                @Embeddable
                data class RecordId(
                    @Column(name = "ID")
                    val id: String,

                    @Column(name = "TYPE")
                    val type: String
                ) {
                    companion object {
                        fun from(id: String, type: String) = RecordId(id, type)
                    }
                }

                @Entity
                data class Record(
                    @EmbeddedId
                    val id: RecordId,
                    val timestamp: LocalDateTime
                )
            """.trimIndent()
        )

        assertCompiles(source)
    }

    @Test
    fun shouldHandleManyToOne() {
        val sourceFile = SourceFile.kotlin(
            "Customer.kt", """
                import javax.persistence.*
                
                @Entity
                data class Customer(
                    @Id
                    val id: Long? = null,
                    val name: String,
                    
                    @ManyToOne
                    @JoinColumn(name = "contact_person_id")
                    val contactPerson: ContactPerson
                )
                
                @Entity
                data class ContactPerson(
                    @Id @GeneratedValue
                    val id: Long? = null,
                
                    @Column(name = "first_name")
                    val firstName: String,
                
                    @Column(name = "last_name")
                    val lastName: String
                )
            """.trimIndent()
        )

        assertCompiles(sourceFile)
    }

    private fun assertCompiles(source: SourceFile) {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(KrushSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }
        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(compilation.kspSourcesDir)
            .isDirectoryRecursivelyContaining { file -> file.name == "mappings.kt" }

    }
}
