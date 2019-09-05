package pl.touk.exposed.generator.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import pl.touk.exposed.generator.AnnotationProcessorTest
import pl.touk.exposed.generator.env.AnnotationEnvironment

class EntityGraphBuilderTest : AnnotationProcessorTest() {

    @Test
    fun shouldPutEntityToGraph() {
        val customerElt = getTypeElement("pl.touk.example.Customer")

        val annEnv = AnnotationEnvironment(listOf(customerElt), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        val graphBuilder = EntityGraphBuilder(getTypeEnv(), annEnv)

        assertThat(graphBuilder.build())
                .containsKey(customerElt)
                .containsValue(
                        EntityDefinition(
                                name = customerElt.simpleName, qualifiedName = customerElt.qualifiedName,
                                table = "customers", id = null)
                )
    }

}

