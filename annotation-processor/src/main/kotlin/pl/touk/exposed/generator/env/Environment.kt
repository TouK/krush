package pl.touk.exposed.generator.env

import java.lang.IllegalArgumentException
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

data class Environment(
        val entities: List<TypeElement>,
        val ids: List<VariableElement>,
        val genValues: List<VariableElement>
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

    fun build(): Environment {
        val entities = roundEnv.getElementsAnnotatedWith(Entity::class.java).toTypeElements()
        val ids = roundEnv.getElementsAnnotatedWith(Id::class.java).toVariableElements()
        val genValues = roundEnv.getElementsAnnotatedWith(GeneratedValue::class.java).toVariableElements()

        return Environment(entities, ids, genValues)
    }

    private fun Collection<Element>.toTypeElements() = this.map(Element::toTypeElement)
    private fun Collection<Element>.toVariableElements() = this.map(Element::toVariableElement)

}
