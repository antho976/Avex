package com.forge.app.core.perf

import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Where a cold start's time goes, as one logcat line per milestone:
 *
 *     I AvexStartup: home.data +183ms
 *
 * Each time is measured from the moment the process was forked, so the lines of one launch add up
 * to a timeline: `adb logcat -s AvexStartup`, or `tools/perf/coldstart.sh` for the medians over
 * several launches. `Displayed` (first frame) and `Fully drawn` (Home showing real data) are the
 * system's own two numbers for the same launch; these marks say what happened in between.
 *
 * A name is logged once per process, the first time it is reached, so a mark may sit on a path that
 * runs again later (a flow's every emission, a recomposition) and cost one set lookup there.
 */
object StartupTimeline {
    const val TAG = "AvexStartup"

    private val processStartMs = Process.getStartUptimeMillis()
    private val seen: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun mark(name: String) {
        if (!seen.add(name)) return
        Log.i(TAG, "$name +${SystemClock.uptimeMillis() - processStartMs}ms")
    }
}
