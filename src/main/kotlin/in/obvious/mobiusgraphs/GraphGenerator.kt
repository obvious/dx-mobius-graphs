package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.core.*
import com.google.common.graph.EndpointPair
import com.google.common.graph.Network
import guru.nidi.graphviz.attribute.*
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.MutableNode
import java.awt.image.BufferedImage

@Suppress("UnstableApiUsage")
class GraphGenerator {

    fun generate(
        logicGraph: Network<State, Event>,
        name: String
    ): BufferedImage {
        val nodes: Map<String, MutableNode> = constructNodes(logicGraph)

        linkNodesWithEdges(nodes, logicGraph)

        val graph = Factory.mutGraph().setDirected(true)

        graph.graphAttrs().apply {
            add(Rank.dir(Rank.RankDir.TOP_TO_BOTTOM))
            add(
                Label.html("<b>$name</b>")
                    .locate(Label.Location.TOP)
            )
        }
        graph.nodeAttrs().add(Shape.ELLIPSE)

        graph.add(nodes.values.toList())

        return Graphviz.fromGraph(graph)
            .height(1000)
            .render(Format.SVG)
            .toImage()
    }

    private fun linkNodesWithEdges(
        nodes: Map<String, MutableNode>,
        logicGraph: Network<State, Event>
    ) {
        logicGraph
            .edges()
            .map { it to logicGraph.incidentNodes(it) }
            .map { (event, endpoint) ->
                mapEdgesToLinks(
                    endpoint,
                    event,
                    findNodeByName = { stateName -> nodes.getValue(stateName) }
                )
            }
            .forEach { (from, link) -> from.addLink(link) }
    }

    private fun constructNodes(logicGraph: Network<State, Event>): Map<String, MutableNode> {
        return logicGraph
            .nodes()
            .map { state -> state.name to mapStateToNode(state) }
            .associateBy({ (name, _) -> name }, { (_, node) -> node })
    }

    private fun mapEdgesToLinks(
        endpoint: EndpointPair<State>,
        event: Event,
        findNodeByName: (String) -> MutableNode
    ): Pair<MutableNode, Link> {
        val from = endpoint.source()
        val to = endpoint.target()

        val sourceNode = findNodeByName(from.name)
        val targetNode = findNodeByName(to.name)

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
        return Pair(sourceNode, link)
    }

    private fun mapStateToNode(state: State): MutableNode {
        val nameLine = "<b>${state.name}</b>"
        val effectLine = if (state.effects.isEmpty()) "" else "<i>${state.effects.joinToString { it.name }}</i>"

        return when (state) {
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
    }
}