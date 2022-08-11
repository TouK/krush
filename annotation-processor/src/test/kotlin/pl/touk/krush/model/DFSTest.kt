package pl.touk.krush.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.touk.krush.AnnotationProcessorTest
import pl.touk.krush.meta.toModelType
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@KotlinPoetMetadataPreview
class DFSTest(types: Types, elements: Elements) : AnnotationProcessorTest(types, elements), EntityGraphSampleData {

    @Test
    fun shouldVisitAllNodesUsingDFS() {
        //given
        val graphBuilder = oneToOneGraphBuilder(getTypeEnv())

        //when
        val graphs = graphBuilder.build()
        val type = oneToOneSourceEntity(getTypeEnv()).toModelType()

        val elements = DFS(graphs).visit(type)

        //then
        assertThat(elements)
            .hasSize(2)
            .extracting("type")
            .containsOnly(type, oneToOneTargetEntity(getTypeEnv()).toModelType())
    }

}
