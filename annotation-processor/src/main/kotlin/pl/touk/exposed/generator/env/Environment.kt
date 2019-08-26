package pl.touk.exposed.generator.env

import java.lang.IllegalArgumentException
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

data class TypeEnvironment(
        val typeUtils: Types,
        val elementUtils: Elements
) {
    fun isSubType(type: TypeMirror, qualifiedName: CharSequence) =
            typeUtils.isSubtype(type, elementUtils.getTypeElement(qualifiedName).asType())

    fun isSameType(type: TypeMirror, qualifiedName: CharSequence) =
            typeUtils.isSameType(type, elementUtils.getTypeElement(qualifiedName).asType())
}

data class AnnotationEnvironment(
        val entities: List<TypeElement>,
        val ids: List<VariableElement>,
        val genValues: List<VariableElement>,
        val columns: List<VariableElement>
)

fun Element.enclosingTypeElement() = this.enclosingElement.toTypeElement()

fun Element.toTypeElement(): TypeElement {
    if (this !is TypeElement) {
        throw IllegalArgumentException("Invalid element type ${this.kind}, type expected")
    }
    return this
}

 fun Element.toVariableElement(): VariableElement {
    if (this !is VariableElement) {
        throw IllegalArgumentException("Invalid element type ${this.kind}, var expected")
    }
    return this
}

class EnvironmentBuilder(private val roundEnv: RoundEnvironment, private val processingEnv: ProcessingEnvironment) {

    fun buildAnnotationEnv(): AnnotationEnvironment {
        val entities = roundEnv.getElementsAnnotatedWith(Entity::class.java).toTypeElements()
        val ids = roundEnv.getElementsAnnotatedWith(Id::class.java).toVariableElements()
        val genValues = roundEnv.getElementsAnnotatedWith(GeneratedValue::class.java).toVariableElements()
        val columns = roundEnv.getElementsAnnotatedWith(Column::class.java).toVariableElements()

        return AnnotationEnvironment(entities, ids, genValues, columns)
    }

    fun buildTypeEnv() = TypeEnvironment(processingEnv.typeUtils, processingEnv.elementUtils)

    private fun Collection<Element>.toTypeElements() = this.map(Element::toTypeElement)
    private fun Collection<Element>.toVariableElements() = this.map(Element::toVariableElement)

}
