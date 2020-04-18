package `in`.obvious.mobiusgraphs

import io.reactivex.rxjava3.core.Observable
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

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