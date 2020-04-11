package `in`.obvious.mobiusgraphs

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Rank
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import guru.nidi.graphviz.model.Factory.*
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

fun main(args: Array<String>) {
    logger().debug("Run with args: ${args.joinToString()}")

    val graphGenerator = GraphGenerator()

    with(JFrame("Mobius Graphs")) {
        val image = graphGenerator.generateAsImage()

        size = Dimension(image.width + 50, image.height + 50)

        add(JLabel(ImageIcon(image), SwingConstants.CENTER))

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        isVisible = true
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
}