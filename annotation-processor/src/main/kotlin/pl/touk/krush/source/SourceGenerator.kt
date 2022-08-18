package pl.touk.krush.source

import com.squareup.kotlinpoet.FileSpec
import pl.touk.krush.env.TypeEnvironment
import pl.touk.krush.model.EntityGraph
import pl.touk.krush.model.EntityGraphs

interface SourceGenerator {

    fun generate(graph: EntityGraph, graphs: EntityGraphs, packageName: String) : FileSpec
}
