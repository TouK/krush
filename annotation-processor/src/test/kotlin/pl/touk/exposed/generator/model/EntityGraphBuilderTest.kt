package pl.touk.exposed.generator.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import pl.touk.exposed.generator.AnnotationProcessorTest
import pl.touk.exposed.generator.env.Environment

class EntityGraphBuilderTest : AnnotationProcessorTest() {

    private val entityGraphBuilder = EntityGraphBuilder()

    @Test
    fun shouldPutEntityToGraph() {
        val customerElt = getTypeElement("pl.touk.example.Customer")

        val env = Environment(listOf(customerElt), emptyList(), emptyList())

        val graph = entityGraphBuilder.build(env)

        assertThat(graph)
                .containsKey(customerElt)
                .containsValue(EntityDefinition(name = customerElt.simpleName, table = "customers", id = null))
    }

}

