package pl.touk.krush

import com.google.testing.compile.CompilationExtension
import org.junit.jupiter.api.extension.ExtendWith
import pl.touk.krush.env.TypeEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@ExtendWith(CompilationExtension::class)
abstract class AnnotationProcessorTest(private val types: Types, private val elements: Elements) {
    fun getTypeEnv() = TypeEnvironment(types, elements)
}
