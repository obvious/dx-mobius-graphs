package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.core.*
import com.google.common.graph.Network
import guru.nidi.graphviz.attribute.*
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.MutableNode
import java.awt.image.BufferedImage

class GraphGenerator {

    @Suppress("UnstableApiUsage")
    fun generate(
        logicGraph: Network<State, Event>,
        name: String
    ): BufferedImage {
        val nodes: Map<String, MutableNode> = logicGraph
            .nodes()
            .map { state ->
                val nameLine = "<b>${state.name}</b>"
                val effectLine = if (state.effects.isEmpty()) "" else "<i>${state.effects.joinToString { it.name }}</i>"

                val node = when (state) {
                    is Concrete -> {
                        val label = if (effectLine.isNotBlank()) Label.htmlLines(
                            Label.Justification.MIDDLE,
                            nameLine,
                            effectLine
                        ) else Label.htmlLines(
                            Label.Justification.MIDDLE,
                            nameLine
                        )
                        Factory.mutNode(label)
                    }
                    is Intermediate -> {
                        val label = Label.htmlLines(
                            Label.Justification.MIDDLE,
                            effectLine
                        )
                        Factory.mutNode(label)
                            .add(Style.DASHED)
                            .add(Color.DIMGRAY)
                    }
                }
                state.name to node
            }
            .associateBy({ (name, _) -> name }, { (_, node) -> node })

        logicGraph
            .edges()
            .map { it to logicGraph.incidentNodes(it) }
            .map { (event, endpoint) ->
                val from = endpoint.source()
                val to = endpoint.target()

                val sourceNode = nodes.getValue(from.name)
                val targetNode = nodes.getValue(to.name)

                val edgeName = when (event) {
                    is ExternalEvent -> Label.of(
                        event.name
                    )
                    is InternalEvent -> Label.of(
                        "${event.source.name}: ${event.name}"
                    )
                }
                val link = sourceNode.linkTo(targetNode).with(edgeName)

                if (event is InternalEvent) {
                    link.add(Color.NAVY).add(Color.NAVY.font())
                }

                sourceNode to link
            }
            .onEach { (from, link) -> from.addLink(link) }
            .map { it.second }

        val graph = Factory.mutGraph().setDirected(true)
        graph.graphAttrs().apply {
            add(Rank.dir(Rank.RankDir.TOP_TO_BOTTOM))
            add(
                Label.html("<b>$name</b>")
                    .locate(Label.Location.TOP))
        }
        graph.nodeAttrs().add(Shape.ELLIPSE)

        graph.add(nodes.values.toList())

        return Graphviz.fromGraph(graph)
            .height(1000)
            .render(Format.SVG)
            .toImage()
    }
}