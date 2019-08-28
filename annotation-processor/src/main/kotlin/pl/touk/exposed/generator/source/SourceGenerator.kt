package pl.touk.exposed.generator.source

import org.yanex.takenoko.KoFile
import pl.touk.exposed.generator.model.EntityGraph

interface SourceGenerator {

    fun generate(graph: EntityGraph, packageName: String) : GeneratedFile
}

data class GeneratedFile(
        val koFile: KoFile,
        val name: String
)
