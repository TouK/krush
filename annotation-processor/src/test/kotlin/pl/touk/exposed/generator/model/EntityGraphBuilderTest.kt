package pl.touk.exposed.generator.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import pl.touk.exposed.generator.AnnotationProcessorTest

class EntityGraphBuilderTest : AnnotationProcessorTest(), EntityGraphSampleData {

    @Test
    fun shouldPutEntityToGraph() {
        //given
        val customerGraphBuilder = customerGraphBuilder(getTypeEnv())

        //when
        val graphs = customerGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(customerTestEntity(getTypeEnv()))
                .containsValue(customerTestEntityDefinition(getTypeEnv()))
    }

    @Test
    fun shouldHandleColumnNaming() {
        //given
        val validColumnMappingGraphBuilder = validTableMappingGraphBuilder(getTypeEnv())

        //when
        val graphs = validColumnMappingGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(customPropertyNameEntity(getTypeEnv()))
                .containsValue(defaultPropertyNameEntityDefinition(getTypeEnv()))
                .containsValue(customPropertyNameEntityDefinition(getTypeEnv()))
    }

    @Test
    fun shouldHandleNullableFields() {
        //given
        val nullablePropertyGraphBuilder = nullablePropertyGraphBuilder(getTypeEnv())

        //when
        val graphs = nullablePropertyGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(nullablePropertyEntity(getTypeEnv()))
                .containsValue(nullablePropertyEntityDefinition(getTypeEnv()))
    }
}

