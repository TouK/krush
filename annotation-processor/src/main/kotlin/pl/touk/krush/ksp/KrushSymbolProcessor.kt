package pl.touk.krush.ksp

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
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

class KrushSymbolProcessor(val env: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Entity::class.java.name)
        val invalid = symbols
            .filter { !it.validate() }.toList()
        val visitor = KrushEntityVisitor()
        symbols.forEach { it.accept(visitor, null) }

        val embeddableVisitor = KrushEmbeddableVisitor()
        resolver.getSymbolsWithAnnotation(Embeddable::class.java.name)
            .forEach { it.accept(embeddableVisitor, null) }

        val annotationEnv = AnnotationEnvironment(
            entities = visitor.entities, ids = visitor.ids, embeddedIds = visitor.embeddedIds,
            columns = visitor.columns, embeddedColumns = visitor.embeddedColumns,
            oneToMany = visitor.oneToMany, manyToOne = visitor.manyToOne,
            manyToMany = visitor.manyToMany, oneToOne = visitor.oneToOne,
            embeddableColumns = embeddableVisitor.columns
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

    internal class KrushEntityVisitor(
        val entities: MutableList<KSClassDeclaration> = mutableListOf(),
        val ids: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val embeddedIds: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val columns: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val embeddedColumns: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val oneToMany: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val manyToOne: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val manyToMany: MutableList<KSPropertyWithClassDeclaration> = mutableListOf(),
        val oneToOne: MutableList<KSPropertyWithClassDeclaration> = mutableListOf()
    ) : KSTopDownVisitor<KSClassDeclaration?, Unit>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KSClassDeclaration?) {
            entities.add(classDeclaration)
            super.visitClassDeclaration(classDeclaration, classDeclaration)
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: KSClassDeclaration?) {
            super.visitPropertyDeclaration(property, data)
            property.getKSAnnotationByType(Id::class)?.let {
                ids.add(property to data!!)
                return
            }
            property.getKSAnnotationByType(EmbeddedId::class)?.let {
                embeddedIds.add(property to data!!)
                return
            }
            property.getKSAnnotationByType(Embedded::class)?.let {
                embeddedColumns.add(property to data!!)
                return
            }
            property.getKSAnnotationByType(OneToMany::class)?.let {
                oneToMany.add(property to data!!)
                return
            }
            property.getKSAnnotationByType(ManyToOne::class)?.let {
                manyToOne.add(property to data!!)
                return
            }

            columns.add(property to data!!)
        }

        override fun defaultHandler(node: KSNode, data: KSClassDeclaration?) {}
    }

    internal class KrushEmbeddableVisitor(
        val columns: MutableList<KSPropertyWithClassDeclaration> = mutableListOf()
    ): KSTopDownVisitor<KSClassDeclaration?, Unit>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: KSClassDeclaration?) {
            super.visitClassDeclaration(classDeclaration, classDeclaration)
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: KSClassDeclaration?) {
            super.visitPropertyDeclaration(property, data)
            columns.add(property to data!!)
        }

        override fun defaultHandler(node: KSNode, data: KSClassDeclaration?) {}
    }
}

class KrushSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = KrushSymbolProcessor(environment)
}
