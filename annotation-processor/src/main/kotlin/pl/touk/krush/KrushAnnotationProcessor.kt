package pl.touk.krush

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import pl.touk.krush.env.EnvironmentBuilder
import pl.touk.krush.model.EntityGraphBuilder
import pl.touk.krush.source.MappingsGenerator
import pl.touk.krush.source.TablesGenerator
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR

@KotlinPoetMetadataPreview
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes("javax.persistence.*")
@SupportedOptions(
        KrushAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME
)
class KrushAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val envBuilder = EnvironmentBuilder(roundEnv, processingEnv)

        val graphs = try {
            EntityGraphBuilder(envBuilder.buildTypeEnv(), envBuilder.buildAnnotationEnv()).build()
        } catch (e: Exception) {
            processingEnv.messager.printMessage(ERROR, "Exception while building entity graph: $e")
            return false
        }

        if (graphs.isEmpty()) return false

        try {
            val generators = listOf(TablesGenerator(), MappingsGenerator())
            generators.forEach { generator ->
                graphs.entries.forEach { (packageName, graph) ->
                    val poetFile = generator.generate(graph, graphs, packageName)
                    poetFile.writeTo(filer = processingEnv.filer)
                }
            }
            return true

        } catch (e: Exception) {
            processingEnv.messager.printMessage(ERROR, "Exception while running KrushAnnotationProcessor: $e")
            return false
        }
    }

}

