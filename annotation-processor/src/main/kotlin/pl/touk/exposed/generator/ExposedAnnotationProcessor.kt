package pl.touk.exposed.generator

import org.jetbrains.exposed.sql.Table
import org.yanex.takenoko.*
import pl.touk.exposed.generator.env.EnvironmentBuilder
import pl.touk.exposed.generator.model.EntityDefinition
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.EntityGraphBuilder
import pl.touk.exposed.generator.model.TypeDefinition
import sun.text.normalizer.UTF16.append
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("javax.persistence.*")
@SupportedOptions(ExposedAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ExposedAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
            processingEnv.messager.printMessage(ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        val envBuilder = EnvironmentBuilder(roundEnv, processingEnv)

        val graph = try {
            EntityGraphBuilder(envBuilder.buildTypeEnv(), envBuilder.buildAnnotationEnv()).build()
        } catch (e: Exception) {
            processingEnv.messager.printMessage(ERROR, "Exception while building entity graph: $e")
            return false
        }

        if (graph.isEmpty()) return false

        try {

            val generatedTableFile = kotlinFile("test.generated") {
                import("org.jetbrains.exposed.sql.Table")

                graph.traverse { typeElement, entity ->
                    objectDeclaration("${typeElement.simpleName}Table") {
                        extends(KoType.parseType(Table::class.java), stringLiteral(entity.table))
                        entity.id?.let { id ->
                            val name = id.name.toString()
                            property(name) {
                                var initializer = "long(\"$name\").primaryKey()"
                                if (id.generatedValue) {
                                    initializer += ".autoIncrement()"
                                }
                                initializer(initializer)
                            }
                        }
                        entity.columns.forEach { column ->
                            val name = column.name.toString()
                            property(name) {
                                val initializer = when (column.type) {
                                    TypeDefinition.STRING -> "varchar(\"$name\", ${column.annotation.length})"
                                    TypeDefinition.LONG -> "long(\"$name\")"
                                    TypeDefinition.BOOL -> "bool(\"$name\")"
                                    TypeDefinition.DATE -> "date(\"name\")"
                                    TypeDefinition.DATETIME -> "datetime(\"name\")"
                                }
                                initializer(initializer)
                            }
                        }
                    }
                }
            }

            File(kaptKotlinGeneratedDir, "tables.kt").apply {
                parentFile.mkdirs()
                writeText(generatedTableFile.accept(PrettyPrinter(PrettyPrinterConfiguration())))
            }

            return true

        } catch (e: Exception) {
            processingEnv.messager.printMessage(ERROR, "Exception while running ExposedAnnotationProcessor: $e")
            return false
        }
    }

}

private fun EntityGraph.traverse(function: (TypeElement, EntityDefinition) -> Unit) {
    this.entries.forEach { (key, value) -> function.invoke(key, value) }
}

