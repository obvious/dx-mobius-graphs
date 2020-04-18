package `in`.obvious.mobiusgraphs

import io.reactivex.rxjava3.core.Observable
import java.io.File
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

fun File.whenChanged(
    pollInterval: Duration = Duration.ofSeconds(1)
): Observable<File> {
    return Observable
        .interval(pollInterval.toMillis(), TimeUnit.MILLISECONDS)
        .takeUntil { !exists() }
        .map { lastModified() }
        .distinctUntilChanged()
        .map { this }
}

fun <T> Observable<T>.sample(duration: Duration): Observable<T> {
    return sample(duration.toMillis(), TimeUnit.MILLISECONDS)
}

class SwingEventDispatcherExecutor : Executor {

    override fun execute(command: Runnable) {
        SwingUtilities.invokeLater(command)
    }
}