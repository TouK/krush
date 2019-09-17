package pl.touk.exposed.generator.source

import com.squareup.kotlinpoet.FileSpec
import pl.touk.exposed.generator.model.EntityGraph
import pl.touk.exposed.generator.model.EntityGraphs

interface SourceGenerator {

    fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String) : FileSpec
}
