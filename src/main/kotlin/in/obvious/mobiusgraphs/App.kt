package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.core.*
import `in`.obvious.mobiusgraphs.dto.LogicDto
import `in`.obvious.mobiusgraphs.mappers.LogicDtoToStateMachine
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.graph.Network
import guru.nidi.graphviz.attribute.*
import guru.nidi.graphviz.attribute.Label.Justification.MIDDLE
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.mutGraph
import guru.nidi.graphviz.model.Factory.mutNode
import guru.nidi.graphviz.model.MutableNode
import io.reactivex.rxjava3.schedulers.Schedulers.*
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.time.Duration
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.system.exitProcess

fun main(@Suppress("UnusedMainParameter") args: Array<String>) {
    with(JFrame("Mobius Graphs")) { pickFile(::renderMobiusGraphFrom, ::exit) }
}

private inline fun JFrame.pickFile(
    onSelected: (File) -> Unit,
    otherwise: () -> Unit
) {
    val chooser = JFileChooser(File(System.getProperty("user.dir"))).apply {
        addChoosableFileFilter(object : FileFilter() {
            private val acceptedExtensions = setOf("yml", "yaml")

            override fun accept(file: File): Boolean {
                return file.isFile && file.extension.toLowerCase() in acceptedExtensions
            }

            override fun getDescription() = "Mobius schema file"
        })
    }
    val chooserResult = chooser.showOpenDialog(this)

    if (chooserResult == JFileChooser.APPROVE_OPTION)
        onSelected(chooser.selectedFile)
    else
        otherwise()
}

private fun JFrame.exit() {
    dispose()
    exitProcess(0)
}

private fun JFrame.renderMobiusGraphFrom(file: File) {
    val graphGenerator = GraphGenerator()
    val logicDtoParser = LogicDtoParser()
    val networkMapper = LogicDtoToStateMachine()

    val iconLabel = JLabel().apply {
        verticalAlignment = SwingConstants.CENTER
        horizontalAlignment = SwingConstants.CENTER
    }
    add(iconLabel, SwingConstants.CENTER)

    file
        .whenChanged()
        .observeOn(io())
        .map(logicDtoParser::fromFile)
        .observeOn(computation())
        .map { logicDto -> networkMapper.map(logicDto) to logicDto.name }
        .map { (graph, name) -> graphGenerator.generate(graph, name) }
        .debounce(Duration.ofSeconds(1))
        .observeOn(from(SwingEventDispatcherExecutor()))
        .subscribe { image ->
            size = Dimension(image.width + 50, image.height + 50)
            iconLabel.icon = ImageIcon(image)
            revalidate()
        }

    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    isVisible = true
}

class LogicDtoParser {

    private val objectMapper = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule())
    }

    fun fromFile(file: File): LogicDto {
        return objectMapper.readValue<LogicDto>(file, LogicDto::class.java)
    }

    fun fromString(data: String): LogicDto {
        return objectMapper.readValue<LogicDto>(data, LogicDto::class.java)
    }
}

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
                            MIDDLE,
                            nameLine,
                            effectLine
                        ) else Label.htmlLines(MIDDLE, nameLine)
                        mutNode(label)
                    }
                    is Intermediate -> {
                        val label = Label.htmlLines(MIDDLE, effectLine)
                        mutNode(label)
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
                    is ExternalEvent -> Label.of(event.name)
                    is InternalEvent -> Label.of("${event.source.name}: ${event.name}")
                }
                val link = sourceNode.linkTo(targetNode).with(edgeName)

                if (event is InternalEvent) {
                    link.add(Color.NAVY).add(Color.NAVY.font())
                }

                sourceNode to link
            }
            .onEach { (from, link) -> from.addLink(link) }
            .map { it.second }

        val graph = mutGraph().setDirected(true)
        graph.graphAttrs().apply {
            add(Rank.dir(Rank.RankDir.TOP_TO_BOTTOM))
            add(Label.html("<b>$name</b>").locate(Label.Location.TOP))
        }
        graph.nodeAttrs().add(Shape.ELLIPSE)

        graph.add(nodes.values.toList())

        return Graphviz
            .fromGraph(graph)
            .height(1000)
            .render(Format.SVG)
            .toImage()
    }
}