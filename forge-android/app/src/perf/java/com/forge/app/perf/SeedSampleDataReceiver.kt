package com.forge.app.perf

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.forge.app.data.repo.SampleDataSeeder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Perf build only: fills an onboarded install with eight weeks of sample training, so a
 * measurement loads a real history instead of empty states. Finish onboarding first, then:
 *
 * `adb shell am broadcast -a com.forge.app.perf.SEED -n com.quietsoftware.avex.perf/com.forge.app.perf.SeedSampleDataReceiver`
 *
 * The debug build has its own copy (src/debug); a build type cannot borrow another's sources.
 */
@AndroidEntryPoint
class SeedSampleDataReceiver : BroadcastReceiver() {

    @Inject lateinit var seeder: SampleDataSeeder

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seeder.seed()
                Log.i(TAG, "Sample data seeded")
            } catch (t: Throwable) {
                Log.e(TAG, "Seeding failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val ACTION_SEED = "com.forge.app.perf.SEED"
        const val TAG = "AvexSeed"
    }
}
