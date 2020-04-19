package `in`.obvious.mobiusgraphs

import `in`.obvious.mobiusgraphs.mappers.LogicDtoToStateMachine
import io.reactivex.rxjava3.schedulers.Schedulers.*
import java.awt.Dimension
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