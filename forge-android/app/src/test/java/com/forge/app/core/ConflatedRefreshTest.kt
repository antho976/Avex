package com.forge.app.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConflatedRefreshTest {

    @Test
    fun `a burst is one read, and it starts without waiting`() = runTest {
        var reads = 0
        val refresh = ConflatedRefresh(backgroundScope) { reads++ }
        repeat(10) { refresh.request() }
        // No time passes: the read used to sit behind a 100 ms debounce.
        runCurrent()
        assertEquals(1, reads)
        assertEquals(0, testScheduler.currentTime)
    }

    @Test
    fun `requests during a read get one fresh read after it`() = runTest {
        var reads = 0
        val gate = CompletableDeferred<Unit>()
        val refresh = ConflatedRefresh(backgroundScope) { reads++; if (reads == 1) gate.await() }
        refresh.request()
        runCurrent()
        assertEquals(1, reads)

        repeat(10) { refresh.request() }
        runCurrent()
        assertEquals("the first read is still running", 1, reads)

        gate.complete(Unit)
        runCurrent()
        assertEquals(2, reads)
    }

    @Test
    fun `a request after a read has finished is read again`() = runTest {
        var reads = 0
        val refresh = ConflatedRefresh(backgroundScope) { reads++ }
        refresh.request()
        runCurrent()
        refresh.request()
        runCurrent()
        assertEquals(2, reads)
    }
}
