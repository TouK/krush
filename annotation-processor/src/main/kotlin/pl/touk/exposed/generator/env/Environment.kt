package pl.touk.exposed.generator.env

import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Transient

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
        val columns: List<VariableElement>,
        val oneToMany: List<VariableElement>,
        val manyToOne: List<VariableElement>,
        val manyToMany: List<VariableElement>,
        val oneToOne: List<VariableElement>,
        val embedded: List<VariableElement>,
        val embeddedColumn: List<VariableElement>
)

fun Element.enclosingTypeElement() = this.enclosingElement.toTypeElement()

fun Element.toTypeElement(): TypeElement {
    require(this is TypeElement) { "Invalid element type ${this.kind}, type expected" }
    return this
}

 fun Element.toVariableElement(): VariableElement {
    require(this is VariableElement) { "Invalid element type ${this.kind}, var expected" }
    return this
}

class EnvironmentBuilder(private val roundEnv: RoundEnvironment, private val processingEnv: ProcessingEnvironment) {

    fun buildAnnotationEnv(): AnnotationEnvironment {
        val entities = roundEnv.getElementsAnnotatedWith(Entity::class.java).toTypeElements()
        val ids = roundEnv.getElementsAnnotatedWith(Id::class.java).toVariableElements()
        val oneToMany = roundEnv.getElementsAnnotatedWith(OneToMany::class.java).toVariableElements()
        val manyToOne = roundEnv.getElementsAnnotatedWith(ManyToOne::class.java).toVariableElements()
        val manyToMany = roundEnv.getElementsAnnotatedWith(ManyToMany::class.java).toVariableElements()
        val oneToOne = roundEnv.getElementsAnnotatedWith(OneToOne::class.java).toVariableElements()
        val columns = (roundEnv.rootElements.asSequence().map(this::toColumnElements).flatten() - (ids + oneToOne + oneToMany + manyToOne + manyToMany)).toList()
        val embedded = roundEnv.getElementsAnnotatedWith(Embedded::class.java).toVariableElements()
        val embeddedColumn = roundEnv.getElementsAnnotatedWith(Embeddable::class.java).asSequence().map(this::toEmbeddedElements).flatten().toList()

        return AnnotationEnvironment(entities, ids, columns, oneToMany, manyToOne, manyToMany, oneToOne, embedded, embeddedColumn)
    }

    private fun toColumnElements(entity: Element) = entity.enclosedElements.filter(this::columnPredicate).map(Element::toVariableElement)

    private fun columnPredicate(element: Element) = element.kind == ElementKind.FIELD &&
            element.enclosingElement.getAnnotation(Entity::class.java) != null &&
            element.getAnnotation(Transient::class.java) == null && element.getAnnotation(Embedded::class.java) == null

    private fun toEmbeddedElements(embeddable: Element) = (embeddable.asType() as DeclaredType).asElement().enclosedElements
            .filter(this::embeddedPredicate).map(Element::toVariableElement)

    private fun embeddedPredicate(element: Element) = element.kind == ElementKind.FIELD &&
            element.enclosingElement.getAnnotation(Embeddable::class.java) != null &&
            element.getAnnotation(Transient::class.java) == null && element.getAnnotation(Embedded::class.java) == null


    fun buildTypeEnv() = TypeEnvironment(processingEnv.typeUtils, processingEnv.elementUtils)

    private fun Collection<Element>.toTypeElements() = this.map(Element::toTypeElement)
    private fun Collection<Element>.toVariableElements() = this.map(Element::toVariableElement)

}
