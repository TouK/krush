package pl.touk.krush.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import pl.touk.krush.AnnotationProcessorTest

@KotlinPoetMetadataPreview
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

    @Test
    fun shouldHandleOneToOneMapping() {
        //given
        val oneToOneGraphBuilder = oneToOneGraphBuilder(getTypeEnv())

        //when
        val graphs = oneToOneGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(oneToOneSourceEntity(getTypeEnv()))
                .containsValue(oneToOneSourceEntityDefinition(getTypeEnv()))
                .containsKey(oneToOneTargetEntity(getTypeEnv()))
                .containsValue(oneToOneTargetEntityDefinition(getTypeEnv()))
    }

    @Test
    fun shouldHandleNumericPropertyTypes() {
        //given
        val numericPropertyGraphBuilder = numericPropertyGraphBuilder(getTypeEnv())

        //when
        val graphs = numericPropertyGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(numericPropertyEntity(getTypeEnv()))
                .containsValue(numericPropertyEntityDefinition(getTypeEnv()))
    }

    @Test
    fun shouldHandleDatePropertyTypes() {
        //given
        val datePropertyGraphBuilder = datePropertyGraphBuilder(getTypeEnv())

        //when
        val graphs = datePropertyGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(datePropertyEntity(getTypeEnv()))
                .containsValue(datePropertyEntityDefinition(getTypeEnv()))
    }

    @Test
    fun shouldHandleEmbeddedPropertyTypes() {
        //given
        val embeddedPropertyGraphBuilder = embeddedPropertyGraphBuilder(getTypeEnv())

        //when
        val graphs = embeddedPropertyGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(embeddedPropertyEntity(getTypeEnv()))
                .containsValue(embeddedPropertyEntityDefinition(getTypeEnv()))
    }

    @Test
    fun shouldHandleEnumPropertyTypes() {
        //given
        val enumPropertyGraphBuilder = enumPropertyGraphBuilder(getTypeEnv())

        //when
        val graphs = enumPropertyGraphBuilder.build()

        //then
        assertThat(graphs).containsKey("pl.touk.example")

        assertThat(graphs["pl.touk.example"])
                .containsKey(enumPropertyEntity(getTypeEnv()))
                .containsValue(enumPropertyEntityDefinition(getTypeEnv()))
    }
}

