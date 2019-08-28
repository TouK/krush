package pl.touk.exposed.generator

import org.jetbrains.exposed.sql.Table
import org.yanex.takenoko.*
import pl.touk.exposed.generator.env.EnvironmentBuilder
import pl.touk.exposed.generator.model.*
import pl.touk.exposed.generator.source.MappingsGenerator
import pl.touk.exposed.generator.source.TablesGenerator
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.persistence.TableGenerator
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
            val generators = listOf(TablesGenerator(), MappingsGenerator())
            val packageName = "generated"
            generators.forEach { generator ->
                val (koFile, fileName) = generator.generate(graph, packageName)
                File(kaptKotlinGeneratedDir, fileName).apply {
                    parentFile.mkdirs()
                    writeText(koFile.accept(PrettyPrinter(PrettyPrinterConfiguration())))
                }
            }
            return true

        } catch (e: Exception) {
            processingEnv.messager.printMessage(ERROR, "Exception while running ExposedAnnotationProcessor: $e")
            return false
        }
    }

}

