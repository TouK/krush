package pl.touk.krush.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import com.squareup.kotlinpoet.ksp.writeTo
import pl.touk.krush.source.MappingsGenerator
import pl.touk.krush.source.TablesGenerator
import javax.persistence.Entity
import javax.persistence.Id

class KrushSymbolProcessor(val env: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Entity::class.java.name)
        val invalid = symbols
            .filter { !it.validate() }.toList()
        val visitor = KrushEntityVisitor()
        symbols.forEach { it.accept(visitor, null) }

        val annotationEnv = AnnotationEnvironment(
            visitor.entities, visitor.ids, visitor.columns,
            emptyList(), emptyList(), emptyList(), emptyList()
        )

        val graphs = EntityGraphBuilder(resolver, annotationEnv).build()

        val generators = listOf(TablesGenerator(), MappingsGenerator())
        generators.forEach { generator ->
            graphs.entries.forEach { (packageName, graph) ->
                val poetFile = generator.generate(graph, graphs, packageName)
                poetFile.writeTo(env.codeGenerator, aggregating = false)
            }
        }

        return invalid
    }

    @OptIn(KspExperimental::class)
    internal class KrushEntityVisitor(
        val entities: MutableList<KSClassDeclaration> = mutableListOf(),
        val ids: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val columns: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
    ) : KSTopDownVisitor<KSClassDeclaration?, Unit>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KSClassDeclaration?) {
            entities.add(classDeclaration)
            super.visitClassDeclaration(classDeclaration, classDeclaration)
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: KSClassDeclaration?) {
            super.visitPropertyDeclaration(property, data)
            property.getAnnotationsByType(Id::class).firstOrNull()?.let {
                ids.add(property to data!!)
                return
            }

            columns.add(property to data!!)
        }

        override fun defaultHandler(node: KSNode, data: KSClassDeclaration?) {
        }

    }
}

class KrushSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = KrushSymbolProcessor(environment)
}
