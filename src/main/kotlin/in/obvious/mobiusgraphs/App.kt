package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.mappers.LogicDtoToStateMachine
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers.from
import io.reactivex.rxjava3.schedulers.Schedulers.io
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.io.File
import java.time.Duration
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.system.exitProcess

fun main(@Suppress("UnusedMainParameter") args: Array<String>) {
    with(JFrame("Mobius Graphs")) {
        layout = BorderLayout()
        pickFile(::renderMobiusGraphFrom, ::exit)
    }
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
    val exportMenuItem = JMenuItem("Export")
    exportMenuItem.addActionListener { event ->
        if (event.source == exportMenuItem) {
            logger().debug("Export file!")
        }
    }

    val menuBar = JMenuBar().apply {
        val menu = JMenu("File").apply {
            add(exportMenuItem)
        }

        add(menu)
    }
    add(menuBar, BorderLayout.NORTH)

    val graphGenerator = GraphGenerator()
    val logicDtoParser = LogicDtoParser()
    val networkMapper = LogicDtoToStateMachine()

    val iconLabel = JLabel().apply {
        verticalAlignment = SwingConstants.CENTER
        horizontalAlignment = SwingConstants.CENTER
    }


    val statusLabelFontSize = 32
    val statusLabelVerticalPadding = 16
    val statusFont = Font(Font.SANS_SERIF, Font.BOLD, statusLabelFontSize)
    val limeGreen = Color.decode("#32cd32")
    val orangeRed = Color.decode("#ff4500")

    val statusLabel = JLabel().apply {
        isOpaque = true
        background = Color.DARK_GRAY
        font = statusFont
        border = BorderFactory.createEmptyBorder(statusLabelVerticalPadding, 0, statusLabelVerticalPadding, 0)
        verticalAlignment = SwingConstants.CENTER
        horizontalAlignment = SwingConstants.CENTER
    }
    add(iconLabel, BorderLayout.CENTER)
    add(statusLabel, BorderLayout.SOUTH)

    file
        .whenChanged()
        .observeOn(io())
        .flatMap { sourceFile ->
            Observable
                .fromCallable {
                    val logicDto = logicDtoParser.fromFile(sourceFile)
                    val logicGraph = networkMapper.map(logicDto)

                    Success(graphGenerator.generate(logicGraph, logicDto.name)) as Result
                }
                .onErrorReturn(::Failure)
        }
        .debounce(Duration.ofSeconds(1))
        .observeOn(from(SwingEventDispatcherExecutor()))
        .subscribe { result ->

            when (result) {
                is Success -> {
                    val image = result.image
                    iconLabel.icon = ImageIcon(image)

                    statusLabel.apply {
                        text = "Updated!"
                        foreground = limeGreen
                    }

                    size = Dimension(image.width, menuBar.height + image.height + statusLabelFontSize + (statusLabelVerticalPadding * 3))
                }
                is Failure -> {
                    logger().error("Parse failure", result.cause)
                    statusLabel.apply {
                        text = "Failed to parse, see logs for details!"
                        foreground = orangeRed
                    }
                }
            }
            revalidate()
        }

    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    isVisible = true
}