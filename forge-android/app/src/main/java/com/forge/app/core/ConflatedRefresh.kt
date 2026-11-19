package com.forge.app.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collapse a burst, serialize reads, and retain one fresh request received during an active read.
 *
 * A burst is answered as soon as it has finished arriving, not a fixed time later. This used to
 * `debounce(100)`: every request waited 100 ms in case another followed, so on each open Home's
 * directive, coach row and body readings did not START loading until 100 ms after they were asked
 * for, and Home showed its starting values for that long. It was the largest single wait in a cold
 * start (measured 2026-11-18: asked at 131 ms, started at 250 ms, done 4 ms later).
 *
 * One turn of the dispatcher is enough to collapse the bursts this exists for (a resume observer
 * replaying ON_RESUME beside the real resume, several callers asking in the same frame): by the
 * time the turn comes round every request of the burst has been made, and they are one request.
 * A request made later, while a read is running, is still kept and answered by one more read.
 *
 * [context] is where the reads run. By default that is [scope]'s own dispatcher, the main thread
 * for a view model. A caller whose read only awaits repositories and publishes to state flows can
 * pass a background dispatcher: the read then starts while the main thread is still composing the
 * screen that asked for it, where it used to queue behind that frame.
 */
internal class ConflatedRefresh(
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
    read: suspend () -> Unit
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch(context) {
            for (request in requests) {
                yield()
                // Whatever arrived during that turn is the same burst; this read answers it too.
                requests.tryReceive()
                read()
            }
        }
    }

    fun request() { requests.trySend(Unit) }
}
