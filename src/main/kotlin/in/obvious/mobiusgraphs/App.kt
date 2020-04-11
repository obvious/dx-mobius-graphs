package `in`.obvious.mobiusgraphs

import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants

fun main(args: Array<String>) {
    logger().debug("Run with args: ${args.joinToString()}")

    with(JFrame("Mobius Graphs")) {
        size = Dimension(640, 480)
        add(JLabel("Hello, World!", SwingConstants.CENTER))

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        isVisible = true
    }
}