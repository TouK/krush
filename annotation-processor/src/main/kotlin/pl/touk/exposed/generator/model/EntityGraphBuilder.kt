package pl.touk.exposed.generator.model

import pl.touk.exposed.generator.GeneratedValueWithoutId
import javax.lang.model.type.TypeMirror
import javax.persistence.Column
import javax.persistence.Table
import pl.touk.exposed.generator.env.*
import javax.lang.model.type.TypeKind


class EntityGraphBuilder(
        private val typeEnv: TypeEnvironment, private val annEnv: AnnotationEnvironment
) {

    fun build(): EntityGraph {
        val graph = EntityGraph()

        for (entityElt in annEnv.entities) {
            val tableAnn = entityElt.getAnnotation(Table::class.java)
            graph[entityElt] = EntityDefinition(name = entityElt.simpleName, qualifiedName = entityElt.qualifiedName, table = tableAnn.name)
        }

        for (idElt in annEnv.ids) {
            val entityType = idElt.enclosingTypeElement()
            graph.computeIfPresent(entityType) { _, entity ->
                entity.copy(id = IdDefinition(idElt.simpleName))
            }
        }

        for (genValueElt in annEnv.genValues) {
            val entityType = genValueElt.enclosingTypeElement()
            graph.computeIfPresent(entityType) { _, entity ->
                val idDefinition = entity.id ?: throw GeneratedValueWithoutId(genValueElt, entityType)
                entity.copy(id = idDefinition.copy(generatedValue = true))
            }
        }

        for (columnElt in annEnv.columns) {
            val entityType = columnElt.enclosingTypeElement()
            graph.computeIfPresent(entityType) { _, entity ->
                val columnAnn = columnElt.getAnnotation(Column::class.java)
                // TODO nullable
//                val isNotNull = columnElt.annotationMirrors.any {
//                    (it as DeclaredType).asElement().toTypeElement().qualifiedName.contentEquals(NotNull::class.java.canonicalName)
//                }
                val type = columnElt.asType().getTypeDefinition()
                val columnDefinition = ColumnDefinition(name = columnElt.simpleName, annotation = columnAnn, type = type)
                entity.addColumn(columnDefinition)
            }
        }

        return graph
    }

    private fun TypeMirror.getTypeDefinition() : TypeDefinition {
        return when {
            isString() -> TypeDefinition.STRING
            isBoolean() -> TypeDefinition.BOOL
            isNumeric() -> TypeDefinition.LONG
            else -> TODO()
        }
    }

    private fun TypeMirror.isString() = typeEnv.isSameType(this, "java.lang.String")

    private fun TypeMirror.isBoolean() = typeEnv.isSameType(this, "java.lang.Boolean") || kind == TypeKind.BOOLEAN

    // TODO float/int/long/double
    private fun TypeMirror.isNumeric() = typeEnv.isSubType(this, "java.lang.Number") ||
            kind in listOf(TypeKind.LONG, TypeKind.INT, TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.SHORT)

}
