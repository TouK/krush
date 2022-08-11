package pl.touk.krush.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import javax.persistence.Entity

class KrushSymbolProcessor : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Entity::class.java.name)
        val invalid = symbols
            .filter { !it.validate() }.toList()
        val visitor = KrushVisitor()
        symbols.forEach { it.accept(visitor, Unit) }
//        visitor.entities.map { EntityDefinition(it.packageName) }
        return invalid
    }

    internal class KrushVisitor(val entities: MutableList<KSClassDeclaration> = mutableListOf()) : KSTopDownVisitor<Unit, Unit>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            entities.add(classDeclaration)
            super.visitClassDeclaration(classDeclaration, data)
        }

        override fun defaultHandler(node: KSNode, data: Unit) {
        }

    }
}
