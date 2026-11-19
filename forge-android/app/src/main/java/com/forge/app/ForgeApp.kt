package com.forge.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.forge.app.core.perf.StartupTimeline
import com.forge.app.data.prefs.SettingsRepository
import com.forge.app.data.repo.ProgramRepository
import com.forge.app.service.ReminderScheduler
import com.forge.app.service.WeeklyRecapWorker
import com.forge.app.service.WorkoutSessionService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import javax.inject.Inject

@HiltAndroidApp
class ForgeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository
    // Lazy: field injection runs on the main thread inside super.onCreate(), before the first
    // Activity exists, and these pull in most of the graph (the wear publisher alone reaches the
    // directive and adaptation repositories and every DAO they read; reset reaches eighteen). None is
    // needed until the IO work below, where each is first resolved.
    @Inject lateinit var programRepository: dagger.Lazy<ProgramRepository>
    @Inject lateinit var reminderScheduler: dagger.Lazy<ReminderScheduler>
    @Inject lateinit var wearStatePublisher: dagger.Lazy<com.forge.app.service.wear.WearStatePublisher>
    @Inject lateinit var resetRepository: dagger.Lazy<com.forge.app.data.repo.ResetRepository>
    @Inject lateinit var workoutRepository: dagger.Lazy<com.forge.app.data.repo.WorkoutRepository>
    @Inject lateinit var adaptationRepository: dagger.Lazy<com.forge.app.data.repo.AdaptationRepository>
    private val startupGate = StartupGate()
    internal suspend fun awaitStorageReady() = startupGate.await()

    /** App-lifetime work that should survive any screen (the wear publisher's collectors, W1; a
     *  confirmed share-to-app import, which a rotation must not cancel half way). */
    internal val appScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        StartupTimeline.mark("app.create")
        super.onCreate()
        StartupTimeline.mark("app.injected")
        installCrashLogger()
        if (isDebuggable()) installStrictMode()
        // The settings file, started now rather than when the first screen asks for it. Everything
        // reads it (the Activity cannot compose real content without it), and loading it took about
        // 40 ms of a launch during which the main thread had nothing else to wait for. It still
        // waits behind the gate below, so a restore is applied before the file is opened.
        appScope.launch(Dispatchers.IO) { runCatching { settingsRepository.preferencesSnapshot() } }
        appScope.launch(Dispatchers.IO) {
            try {
                applyPendingRestore()
                // Before the gate opens, so no screen reads a half-wiped database. Fail-soft: a
                // reset that can't finish must not stop the app from starting.
                // The marker is checked here first: asking Hilt for the repository builds the
                // database and seventeen other dependencies, which held the gate (and so the first
                // read of the settings) shut on every launch to learn that there was nothing to do.
                if (File(filesDir, com.forge.app.data.repo.ResetRepository.PENDING_FACTORY_RESET).exists()) {
                    runCatching { resetRepository.get().finishInterruptedFactoryReset() }
                }
                startupGate.complete()
                StartupTimeline.mark("storage.ready")
                startAppServices()
            } catch (failure: Exception) {
                startupGate.fail(failure)
                runCatching { writeCrashLog(failure) }
            }
        }
    }

    private val launchWarmed = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * A launch into the UI has begun: build what Home's first answer is made of now, on background
     * threads, instead of when Home composes.
     *
     * Today's directive and the coach row are both read off the whole-history coach snapshot, and
     * building it was the longest thing a launch waited for: Home asked for it only once it had
     * composed, and showed its starting values until it arrived. Started here it is under way
     * while the Activity is still inflating, and Home's own request joins the build in flight
     * (AdaptationRepository.snapshotCached shares one build between callers).
     *
     * Called by the Activity, not from [onCreate]: a process started for the widget, a worker or a
     * wrist command has no Home to answer, and would load the whole history for nothing.
     */
    internal fun warmForLaunch() {
        if (!launchWarmed.compareAndSet(false, true)) return
        appScope.launch {
            runCatching {
                awaitStorageReady()
                // A snapshot built before the program has loaded describes the seed split, and
                // would be thrown away. [startAppServices] is what loads it.
                com.forge.app.program.Program.readiness
                    .first { it != com.forge.app.program.Program.Readiness.PENDING }
                adaptationRepository.get().snapshotCached()
                StartupTimeline.mark("snapshot.warm")
            }
        }
    }

    private fun startAppServices() {
        WorkoutSessionService.createChannels(this)
        WeeklyRecapWorker.schedule(this)
        // Seed-if-empty + load the DB-backed active program into the Program facade (program-unlock Phase 1).
        // Re-arm the daily training reminder from its persisted setting (no-op when it's off).
        appScope.launch(Dispatchers.IO) {
            // ensureLoaded marks the program FAILED before rethrowing, and every later reader retries
            // the load, so a boot-time failure is logged here rather than crashing the process from
            // this root coroutine — and the reminder below is still re-armed.
            runCatching { programRepository.get().ensureLoaded() }.onFailure { runCatching { writeCrashLog(it) } }
            StartupTimeline.mark("program.loaded")
            // Finish any regeneration that died between its program transaction and the deload
            // marker beside it (M-06). Two stores, no shared transaction: this is what stops them
            // disagreeing forever. Fail-soft — a reconciliation that throws must not stop startup.
            runCatching { programRepository.get().reconcilePendingGeneration() }
            // A fixed-weekday schedule set before rest days were recorded starts its record today.
            runCatching { settingsRepository.ensureScheduleHistory() }
            // A workout left open before a break is closed now, not when the next one starts, so
            // the feed and the widget stop offering to resume it. Kept work goes to history at its
            // last set, and the feed says so once.
            runCatching { workoutRepository.get().closeStaleSession() }.getOrNull()
                ?.takeIf { it.finishedToHistory }
                ?.let { closed ->
                    val day = com.forge.app.program.Program.dayDisplayName(closed.dayKey)
                    val date = java.time.Instant.ofEpochMilli(closed.startedAtMs)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault()))
                    runCatching {
                        settingsRepository.addSystemNotice(
                            com.forge.app.data.repo.NotificationFeed.NOTICE_ORPHAN_SESSION,
                            "Saved your unfinished $day from $date to history. Today starts fresh."
                        )
                    }
                }
            reminderScheduler.get().ensureScheduled(
                settingsRepository.trainingReminderEnabled.first(),
                settingsRepository.trainingReminderHour.first()
            )
        }
        // The user's custom (freestyle-created) exercises into their facade, the way the program is
        // loaded into Program above: the muscle/name resolvers are synchronous and process-wide, so
        // the registry has to be, too. Collected for the app's life so a create/save is reflected.
        appScope.launch {
            settingsRepository.customExercises.collect { com.forge.app.program.CustomExerciseRegistry.setAll(it) }
        }
        // Mirror phone state to the wrist (W1): /session/live + /timer/state + /config collectors,
        // and the app-open /glance/today refresh. All fail-soft — no watch means unread DataItems.
        wearStatePublisher.get().start(appScope)
    }

    /**
     * If a restore staged a database file (see BackupRepository.restoreFromUri), swap it in NOW —
     * before Room is ever opened. Doing the swap at boot avoids closing/replacing the DB while
     * flows are still reading it (the old restore path raced with active observers until exit).
     * A #14 backup also stages the DataStore preferences file; swap that in too, before DataStore
     * is first read (it's lazily created on first access, which happens after this).
     */
    private fun applyPendingRestore() {
        val liveDb = getDatabasePath("forge.db")
        // Carrying on when this returns false is safe BECAUSE of what RestoreApply guarantees: it
        // never returns having left a mixture. Either the whole set is live, or none of it is and
        // the set is queued to retry — so the boot below always runs on one coherent dataset, and
        // the user is told nothing rather than told something untrue.
        if (!RestoreApply.apply(filesDir, liveDb)) {
            check(!RestoreApply.hasUnsettledRecovery(filesDir)) { "Restore recovery needs another attempt" }
            return
        }

        // The set is live, but its pre-restore snapshots are still on disk. The restored database
        // is proven only once THIS configuration has opened it — migrations, identity check and a
        // read — so that happens here, before anything else touches Room, and the snapshots are
        // released or put back on the answer. Staging already opened the file through the same
        // builder, so a failure here means the bytes were damaged in between; it used to mean an
        // app that could not start, with the previous database already discarded.
        // A disposable validator, closed BEFORE rollback. Never initialize or close the app's
        // singleton Room instance while its underlying file is still being selected.
        val validator = com.forge.app.data.db.forgeDatabaseBuilder(this, liveDb.path).build()
        val opened = try {
            runCatching {
                validator.openHelper.writableDatabase.query("SELECT count(*) FROM session")
                    .use { it.moveToFirst() }
            }.isSuccess
        } finally { validator.close() }
        if (opened) {
            RestoreApply.confirm(filesDir, liveDb)
            // The restored database hands out its own session ids again; marks kept for an id from
            // before the restore would attach to the next session that reuses it.
            com.forge.app.service.wear.clearSessionMarks(this)
            // One-shot flag so MainActivity can confirm the restore landed on this launch — the swap
            // runs at boot before any UI exists, and the staging path restarts the process silently
            // otherwise. Only written for a fully clean, opened restore so we never claim
            // "successfully" on a partial one.
            runCatching { File(filesDir, RESTORE_DONE_FLAG).writeText("1") }
        } else {
            check(RestoreApply.revert(filesDir, liveDb)) { "Could not finish restore rollback" }
            runCatching { File(filesDir, RESTORE_FAILED_FLAG).writeText("1") }
        }
    }

    /**
     * Write every uncaught exception to a local file before the process dies, then hand off
     * to the platform handler so the crash still surfaces normally. Offline-friendly forensics:
     * a crash becomes "here's the stack trace" instead of a silent close. Logs live in
     * `filesDir/crashes/`; only the 10 most recent are kept.
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashLog(throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(t: Throwable) {
        val dir = File(filesDir, com.forge.app.data.repo.BackupRepository.CRASH_LOG_DIR).apply { mkdirs() }
        val trace = StringWriter().also { t.printStackTrace(PrintWriter(it)) }.toString()
        File(dir, "crash_${System.currentTimeMillis()}.txt")
            .writeText("Avex crash @ ${Date()}\n\n$trace")
        // Keep only the 10 most recent crash logs.
        dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(10)
            ?.forEach { it.delete() }
    }

    /** Surfaces accidental main-thread disk/network and leaked DB cursors/closeables in debug. */
    private fun installStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    companion object {
        /** One-shot marker written after a boot-time restore swap; read + cleared by MainActivity. */
        const val RESTORE_DONE_FLAG = "restore_just_completed"
        /** One-shot marker written when a landed restore could not be opened and was put back. */
        const val RESTORE_FAILED_FLAG = "restore_put_back"
    }
}
