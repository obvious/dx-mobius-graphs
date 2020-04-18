package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.core.Concrete
import `in`.obvious.mobiusgraphs.core.ExternalEvent
import `in`.obvious.mobiusgraphs.core.Intermediate
import `in`.obvious.mobiusgraphs.core.InternalEvent
import `in`.obvious.mobiusgraphs.dto.LogicDto
import `in`.obvious.mobiusgraphs.mappers.LogicDtoToStateMachine
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import guru.nidi.graphviz.attribute.*
import guru.nidi.graphviz.attribute.Label.Justification.MIDDLE
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.*
import guru.nidi.graphviz.model.MutableNode
import io.reactivex.rxjava3.schedulers.Schedulers.from
import io.reactivex.rxjava3.schedulers.Schedulers.io
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import java.time.Duration
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    logger().debug("Run with args: ${args.joinToString()}")

    val graphGenerator = GraphGenerator()

    with(JFrame("Mobius Graphs")) {
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

        if (chooserResult == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile

            val iconLabel = JLabel()
            add(iconLabel, SwingConstants.CENTER)

            file
                .whenChanged()
                .sample(Duration.ofSeconds(1))
                .map { it.absolutePath }
                .observeOn(io())
                .map(graphGenerator::generateFromFile)
                .observeOn(from(SwingEventDispatcherExecutor()))
                .subscribe { image ->
                    size = Dimension(image.width + 50, image.height + 50)
                    iconLabel.icon = ImageIcon(image)
                    revalidate()
                }

            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            isVisible = true
        } else {
            dispose()
            exitProcess(0)
        }
    }
}

class GraphGenerator {

    fun generateAsImage(): BufferedImage {
        val graph = graph("Sample")
            .directed()
            .graphAttr()
            .with(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
            .with(
                node("a").with(Color.RED).link(node("b")),
                node("b").link(node("c")).with(Style.DASHED),
                node("c")
            )

        return Graphviz
            .fromGraph(graph)
            .height(100)
            .render(Format.SVG)
            .toImage()
    }

    @Suppress("UnstableApiUsage")
    fun generateFromFile(path: String): BufferedImage {
        val objectMapper = ObjectMapper(YAMLFactory()).apply {
            registerModule(KotlinModule())
        }

        val logicDto = objectMapper.readValue<LogicDto>(File(path), LogicDto::class.java)
        val logicGraph = LogicDtoToStateMachine().map(logicDto)

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
            add(Label.html("<b>${logicDto.name}</b>").locate(Label.Location.TOP))
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