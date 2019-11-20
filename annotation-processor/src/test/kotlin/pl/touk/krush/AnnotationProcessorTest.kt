package pl.touk.krush

import com.google.testing.compile.CompilationRule
import org.junit.Before
import org.junit.Rule
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.env.toVariableElement
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AnnotationProcessorTest {

    @Rule
    @JvmField
    var rule = CompilationRule()

    lateinit var elements: Elements
    lateinit var types: Types

    @Before
    fun setup() {
        elements = rule.elements
        types = rule.types
    }

    fun getTypeEnv() = TypeEnvironment(types, elements)

    fun getTypeElement(name: String): TypeElement = elements.getTypeElement(name)

    fun getVariableElement(type: String, name: String): VariableElement = getVariableElement(getTypeElement(type), name)

    fun getVariableElement(typeElt: TypeElement, name: String): VariableElement =
            elements.getAllMembers(typeElt)
                    .filter { it.simpleName.contentEquals(name) }
                    .map(Element::toVariableElement)
                    .first()

}

