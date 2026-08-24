package com.forge.app.data.repo

import com.forge.app.core.time.Clock
import com.forge.app.data.db.dao.CardioDao
import com.forge.app.data.db.dao.LoggedExerciseDao
import com.forge.app.data.db.dao.LoggedSetDao
import com.forge.app.data.db.dao.SessionDao
import com.forge.app.data.db.projections.SetWithExerciseId
import com.forge.app.domain.adapt.E1rm
import com.forge.app.domain.adapt.bestWorkingE1rm
import com.forge.app.program.Program
import com.forge.app.data.db.entities.Session
import com.forge.app.data.db.entities.durationMinutes
import com.forge.app.ui.gym.stats.state.DayTypeVolumeStats
import com.forge.app.ui.gym.stats.state.WeekActivityRow
import com.forge.app.ui.gym.stats.state.ExerciseFrequency
import com.forge.app.ui.gym.stats.state.PeriodComparison
import com.forge.app.ui.gym.stats.state.PeriodStats
import com.forge.app.ui.gym.stats.state.HistoryPoint
import com.forge.app.ui.gym.stats.state.BodyweightPoint
import com.forge.app.ui.gym.stats.state.E1rmLift
import com.forge.app.ui.gym.stats.state.RepMaxSet
import com.forge.app.ui.gym.stats.state.MuscleSetCount
import com.forge.app.ui.gym.stats.state.RepRangeDist
import com.forge.app.ui.gym.stats.state.RpeBucket
import com.forge.app.ui.gym.stats.state.LifetimeMetrics
import com.forge.app.ui.gym.stats.state.MuscleVolume
import com.forge.app.ui.gym.stats.state.OverloadSummary
import com.forge.app.ui.gym.stats.state.PatternAxis
import com.forge.app.ui.gym.stats.state.PrEntry
import com.forge.app.ui.gym.stats.state.PrRecency
import com.forge.app.ui.gym.stats.state.PrRecord
import com.forge.app.ui.gym.stats.state.TimeToPrEntry
import com.forge.app.ui.gym.stats.state.TrainingTimes
import com.forge.app.ui.gym.stats.state.WeeklyDuration
import com.forge.app.ui.gym.stats.state.WeeklyEffortCounts
import com.forge.app.ui.gym.stats.state.WeeklyTonnage
import com.forge.app.ui.gym.session.state.ExerciseDetail
import com.forge.app.ui.gym.session.state.SessionDetailData
import com.forge.app.ui.gym.session.state.SetDetail
import com.forge.app.ui.overview.state.OnThisDayMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates the rolling-window stats that feed the Overview screen.
 *
 * "This week" means the current ISO calendar week (Mon 00:00), matching the day-dot grid — its
 * start is fixed when the flow is subscribed, so reopen the screen after a week rollover. The
 * rolling-7-day volume window in [observeGymStats], by contrast, is recomputed on each emission
 * so it slides while the screen stays open.
 *
 * The pure (DAO-free) aggregation helpers used by [observeGymStats] live in sibling
 * files: StatsStrengthAggregations.kt, StatsVolumeAggregations.kt, and
 * StatsEffortAggregations.kt. The two comparison helpers below stay here because they
 * query the DAO.
 */
@Singleton
class StatsRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val cardioDao: CardioDao,
    private val loggedExerciseDao: LoggedExerciseDao,
    private val loggedSetDao: LoggedSetDao,
    private val vacationDao: com.forge.app.data.db.dao.VacationDao,
    private val bodyweightRepo: BodyweightRepository,
    private val settingsRepo: com.forge.app.data.prefs.SettingsRepository,
    private val clock: Clock
) {

    data class WeeklyStats(
        val workouts: Int = 0,
        val volumeLb: Double = 0.0,
        val cardioMinutes: Int = 0,
        val totalFinishedSessions: Int = 0,
        val streakDays: Int = 0,
        val firstFinishedSessionMs: Long? = null,
        /** Highest single-session volume (lb) in the current ISO week; null when none logged yet. */
        val bestSessionThisWeekLb: Double? = null,
        /** 0=Mon..6=Sun indices that had a finished gym session in the current ISO week. */
        val weekDaysTrained: Set<Int> = emptySet(),
        /** Next gym day key in the rotation (Upper A → Lower A → Upper B → Lower B). */
        val nextUpDayKey: String = Program.UPPER_A,
        /** Last 5 finished gym sessions for the overview RECENT section. */
        val recentGymSessions: List<com.forge.app.data.db.entities.Session> = emptyList()
    )

    fun observeWeeklyStats(): Flow<WeeklyStats> {
        // "This week" = the current ISO calendar week (Mon 00:00), so the workout/volume/cardio
        // counts match the Mon–Sun day-dot grid and buildWeekComparison. Was a rolling 7×24h
        // window (now − WEEK_MS), which disagreed with the dots on early weekdays.
        val zone = ZoneId.systemDefault()
        val weekStartMs = LocalDate.now(zone)
            .let { it.minusDays(it.dayOfWeek.value.toLong() - 1) }
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val baseFlow = combine(
            sessionDao.observeFinishedCountSince(weekStartMs),
            sessionDao.observeVolumeSince(weekStartMs),
            cardioDao.observeMinutesSince(weekStartMs, excludeType = "rest"),
            sessionDao.observeFinishedCount(),
            combine(
                sessionDao.observeRecent(120),
                settingsRepo.scheduleMode,
                settingsRepo.weeklySchedule
            ) { recent, mode, schedule -> Triple(recent, mode, schedule) }
        ) { workouts, volume, cardio, totalFinished, recentModeSchedule ->
            val (recentSessions, scheduleMode, schedule) = recentModeSchedule
            val todayDate = LocalDate.now(zone)
            val finishedAts = recentSessions.mapNotNull { it.finishedAt }
            // Sessions finished in the current ISO week — shared by the lit-day dots AND the
            // best-session tile so they filter the list once, not twice.
            val thisWeekSessions = recentSessions.filter { it.finishedAt != null && it.finishedAt!! >= weekStartMs }
            // Dots use the SAME (subscription-time) week anchor as the workout/volume/cardio counts
            // above, so the count and the lit dots can never describe different weeks within a view.
            val weekDaysTrained = thisWeekSessions
                .map {
                    // Bucket by the same timestamp the week filter uses (finishedAt), so a session
                    // that started before midnight but finished this week lands on the right day.
                    val d = Instant.ofEpochMilli(it.finishedAt!!).atZone(zone).toLocalDate()
                    d.dayOfWeek.value - 1 // 0=Mon..6=Sun
                }
                .toSet()
            val lastFinished = recentSessions.filter { it.finishedAt != null }.maxByOrNull { it.finishedAt!! }
            // Calendar-aware in weekday mode, legacy day-after-last otherwise (shared resolver).
            val trainedTodayKeys = recentSessions
                .filter { it.finishedAt != null && Instant.ofEpochMilli(it.finishedAt!!).atZone(zone).toLocalDate() == todayDate }
                .map { it.dayKey }.toSet()
            val nextUpDayKey = com.forge.app.domain.schedule.WeeklySchedule.resolveNextUp(
                mode = scheduleMode, todayIndex = todayDate.dayOfWeek.value - 1, schedule = schedule,
                dayKeys = Program.dayKeys, lastFinishedDayKey = lastFinished?.dayKey,
                trainedTodayKeys = trainedTodayKeys
            ) ?: (Program.dayKeys.firstOrNull() ?: Program.UPPER_A)
            // Best single session this ISO week — the heaviest tonnage day, for the StatsTile.
            val bestSessionThisWeekLb = thisWeekSessions.mapNotNull { it.totalVolumeLb }.maxOrNull()
            // streakDays is finalized below, once the vacation ranges are known (#135).
            WeeklyStats(
                workouts = workouts,
                volumeLb = volume ?: 0.0,
                cardioMinutes = cardio ?: 0,
                totalFinishedSessions = totalFinished,
                streakDays = 0,
                bestSessionThisWeekLb = bestSessionThisWeekLb,
                weekDaysTrained = weekDaysTrained,
                nextUpDayKey = nextUpDayKey,
                recentGymSessions = recentSessions.filter { it.finishedAt != null }.take(5)
            ) to finishedAts
        }
        return baseFlow
            .combine(sessionDao.observeFirstFinishedSessionStartedAt()) { (stats, ats), firstMs ->
                stats.copy(firstFinishedSessionMs = firstMs) to ats
            }
            .combine(vacationDao.observeAll()) { (stats, ats), periods ->
                // Vacation days bridge the streak — a planned holiday doesn't reset it (#135).
                stats.copy(streakDays = computeStreak(ats, com.forge.app.domain.vacation.VacationCalendar.onVacation(periods)))
            }
            .flowOn(Dispatchers.Default)
    }

    private fun computeStreak(finishedAts: List<Long>, onVacation: (LocalDate) -> Boolean): Int {
        if (finishedAts.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val trainingDays = finishedAts.mapTo(mutableSetOf()) {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }
        // Skip vacation days at the tip so being on holiday *today* doesn't zero an active streak.
        var tip = today
        while (onVacation(tip) && !trainingDays.contains(tip)) tip = tip.minusDays(1)
        val anchor = when {
            trainingDays.contains(tip) -> tip
            trainingDays.contains(tip.minusDays(1)) -> tip.minusDays(1) // one rest-day grace
            else -> return 0
        }
        var streak = 0
        var day = anchor
        while (true) {
            when {
                trainingDays.contains(day) -> { streak++; day = day.minusDays(1) }
                onVacation(day) -> day = day.minusDays(1) // bridge: no break, no increment
                else -> return streak
            }
        }
    }

    /**
     * The day-streak as a one-shot read — same logic and data source as [observeWeeklyStats]'s
     * streakDays, but WITHOUT subscribing the whole weekly fan-out (sessions/volume/cardio/schedule/
     * vacation flows) just to extract one number. For callers like the Profile hub that only need
     * the streak: two focused reads (recent finished sessions + vacation periods) instead of nine.
     */
    suspend fun currentStreakDays(): Int {
        val finishedAts = sessionDao.observeRecent(120).first().mapNotNull { it.finishedAt }
        val periods = vacationDao.all()
        return computeStreak(finishedAts, com.forge.app.domain.vacation.VacationCalendar.onVacation(periods))
    }

    // ─── History / comparison helpers ─────────────────────────────────────────

    fun observeAllFinishedSessions(): Flow<List<Session>> = sessionDao.observeAllFinishedSessions()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeDayVolumeStats(): Flow<Map<String, SessionDao.DayVolumeStats>> =
        sessionDao.observeFinishedCount()
            .flatMapLatest { flow { emit(sessionDao.avgMaxVolumeByDayKey().associateBy { it.dayKey }) } }

    data class SessionExerciseLine(
        val exerciseName: String,
        val topWeightLb: Double?,
        val topReps: Int?,
        val setCount: Int
    )

    suspend fun getSessionExerciseLines(sessionId: Long): List<SessionExerciseLine> {
        val exercises = loggedExerciseDao.forSession(sessionId)
        val allSets = loggedSetDao.allForSession(sessionId)
        val setsByExId = allSets.groupBy { it.loggedExerciseId }
        return exercises.mapNotNull { ex ->
            if (ex.skipped) return@mapNotNull null
            val sets = setsByExId[ex.id] ?: emptyList()
            if (sets.isEmpty()) return@mapNotNull null
            val topSet = sets.maxByOrNull { it.weightLb ?: 0.0 }
            // Resolves swap name → active plan → seed split → library, with a humanized last-ditch
            // fallback so a swapped/rotated-out/removed id never surfaces as a raw kebab key (C3).
            val name = Program.exerciseDisplayName(ex.exerciseId, ex.swappedName)
            SessionExerciseLine(
                exerciseName = name,
                topWeightLb = topSet?.weightLb,
                topReps = topSet?.reps,
                setCount = sets.size
            )
        }
    }

    /**
     * The full per-session breakdown that drives the session-detail page: the session row's
     * aggregates plus every non-skipped exercise with its sets. Pure read — assembled off the
     * main thread by the caller. Returns null if the session id no longer exists.
     */
    suspend fun getSessionDetail(sessionId: Long): SessionDetailData? {
        val session = sessionDao.get(sessionId) ?: return null
        val exercises = loggedExerciseDao.forSession(sessionId)
        val setsByExId = loggedSetDao.allForSession(sessionId).groupBy { it.loggedExerciseId }

        // Prior-best e1RM per exercise: ONE query for the whole session, restricted to sessions that
        // STARTED before this one — so an old session reflects what was a best AT THE TIME (not all-time),
        // and so we don't fire a frontier query per exercise (was an N+1).
        val priorFrontier = run {
            val targetIds = exercises.filterNot { it.skipped }.map { it.exerciseId }.distinct()
            if (targetIds.isEmpty()) emptyMap()
            else loggedSetDao.repMaxFrontierBeforeSession(targetIds, session.startedAt).groupBy { it.exerciseId }
        }

        val exerciseDetails = exercises.mapNotNull { ex ->
            if (ex.skipped) return@mapNotNull null
            val sets = (setsByExId[ex.id] ?: emptyList()).sortedBy { it.setIndex }
            if (sets.isEmpty()) return@mapNotNull null
            val topWeight = sets.mapNotNull { it.weightLb }.maxOrNull()
            // Mark only ONE set as the top set (the first at the heaviest weight), not the whole
            // top-weight cluster — straight sets at the same weight shouldn't all read as "the" top.
            val topIdx = if (topWeight == null) -1 else sets.indexOfFirst { it.weightLb == topWeight }
            // Resolves swap name → active plan → seed split → library, with a humanized last-ditch
            // fallback so a swapped/rotated-out/removed id never surfaces as a raw kebab key (C3).
            val name = Program.exerciseDisplayName(ex.exerciseId, ex.swappedName)
            val setDetails = sets.mapIndexed { i, s ->
                SetDetail(
                    number = i + 1,
                    weightLb = s.weightLb,
                    weightText = s.weightText,
                    reps = s.reps,
                    rpe = s.rpe,
                    isTopSet = i == topIdx,
                    isAmrap = s.isAmrap,
                    toFailure = s.toFailure,
                    isAssisted = s.isAssisted,
                    dropAnnotation = s.dropAnnotation,
                    setType = s.setType,
                    durationSeconds = s.durationSeconds
                )
            }
            // Best working e1RM this session, flagged as a new best when it tops every PRIOR session's
            // best for the same exercise (prior frontier batched above, restricted to earlier sessions).
            val e1rm = bestWorkingE1rm(sets)
            val priorBestE1rm = priorFrontier[ex.exerciseId]
                ?.maxOfOrNull { E1rm.epley(it.weightLb, it.reps) }
            ExerciseDetail(
                name = name,
                isPr = ex.wasPr,
                effort = ex.difficulty,
                hitFullTarget = ex.hitFullTarget,
                note = ex.note?.takeIf { it.isNotBlank() },
                topWeightLb = topWeight,
                totalReps = setDetails.sumOf { it.reps },
                volumeLb = setDetails.sumOf { it.volumeLb },
                e1rmLb = e1rm,
                e1rmIsBest = e1rm != null && (priorBestE1rm == null || e1rm > priorBestE1rm + 1e-6),
                sets = setDetails
            )
        }

        // Muscle split: fold each exercise's WORKING-set count (assisted sets excluded, matching the
        // "working sets" label) onto the muscle its library entry maps to.
        val muscleSplit = buildSessionMuscleSplit(
            exercises
                .filterNot { it.skipped }
                .mapNotNull { ex -> (setsByExId[ex.id]?.count { !it.isAssisted } ?: 0).takeIf { it > 0 }?.let { ex.exerciseId to it } }
        )

        val allRpe = exerciseDetails.flatMap { it.sets }.mapNotNull { it.rpe }
        val durationMin = session.durationMinutes()
        val title = Program.dayDisplayName(session.dayKey)
        // The previous session of this same training, for the summary-tile up/down/same carets. The DAO
        // returns the most-recent OTHER session of this day_key regardless of date, so drop it when it
        // actually finished LATER (i.e. we're viewing an older session from History) — otherwise the
        // caret would compare against a future session and invert.
        val curFinishedAt = session.finishedAt ?: Long.MAX_VALUE
        val prevSession = sessionDao.previousFinishedForDay(session.dayKey, session.id)
            ?.takeIf { (it.finishedAt ?: Long.MAX_VALUE) < curFinishedAt }

        return SessionDetailData(
            sessionId = session.id,
            title = title,
            dateMs = session.startedAt,
            tag = session.tags,
            durationMin = durationMin,
            volumeLb = session.totalVolumeLb,
            prCount = session.prCount,
            setCount = session.setCount,
            prevVolumeLb = prevSession?.totalVolumeLb,
            prevSetCount = prevSession?.setCount,
            intensity = session.intensity,
            sessionType = session.sessionType,
            deload = session.deloadMarkedHere,
            journal = session.journal,
            avgRpe = if (allRpe.isEmpty()) null else allRpe.average(),
            muscleSplit = muscleSplit,
            exercises = exerciseDetails
        )
    }

    // ─── Gym stats subtab ──────────────────────────────────────────────────────

    /**
     * Snapshot feeding the rebuilt Gym → Stats page. The page renders every field on every open at
     * its honest zero (nothing there unlocks), so this snapshot is deliberately WIDE: it calls the
     * tested aggregation helpers that the previous four-tab screen defined but never rendered.
     *
     * Everything here is folded from the two already-subscribed flows plus a handful of aggregate
     * queries fired concurrently, so the added breadth costs queries, not per-set work.
     */
    data class GymStats(
        // ── Hero ────────────────────────────────────────────────────────────────────────────────
        /** This-ISO-week vs last-week side by side. */
        val weekComparison: PeriodComparison? = null,
        /** Weekly average of each tracked lift's best e1RM — the hero verdict and its sparkline. */
        val overload: OverloadSummary? = null,

        // ── SHOW UP ─────────────────────────────────────────────────────────────────────────────
        val consistencyStreak: Int = 0,
        /** Sessions per ISO week, last 12 weeks, oldest → newest. */
        val weeklySessionCounts: List<Int> = emptyList(),
        /** Median session length per ISO week, oldest → newest. */
        val weeklyDurations: List<WeeklyDuration> = emptyList(),
        /** Distinct sessions per exercise over the last 8 weeks, ranked. */
        val exerciseFrequency: List<ExerciseFrequency> = emptyList(),

        // ── STRONGER ────────────────────────────────────────────────────────────────────────────
        val recentPrs: List<PrEntry> = emptyList(),
        val e1rmLifts: List<E1rmLift> = emptyList(),
        /** Per-exercise load-rep scatter + e1RM for the strength curve. */
        val strengthCurves: List<com.forge.app.ui.gym.stats.state.StrengthCurve> = emptyList(),
        /** Days since the last PR, overall and per exercise. */
        val prRecency: PrRecency? = null,
        /** Average days between PRs per exercise. */
        val timeToPr: List<TimeToPrEntry> = emptyList(),
        /** Dated bodyweight points, oldest → newest. */
        val bodyweightPoints: List<BodyweightPoint> = emptyList(),
        /** Best weight at each rep count on the most-trained lift. */
        val repMaxes: RepMaxSet? = null,
        /** Per movement pattern: recent-window best e1RM against the all-time peak. */
        val patternAxes: List<PatternAxis> = emptyList(),

        // ── ENOUGH ──────────────────────────────────────────────────────────────────────────────
        val weeklySetsByMuscle: List<MuscleSetCount> = emptyList(),
        /** Planned weekly sets per muscle from the active program. */
        val plannedSetsByMuscle: Map<com.forge.app.program.MuscleGroup, Int> = emptyMap(),
        /** Every logged set split across strength / hypertrophy / endurance rep ranges. */
        val repRange: RepRangeDist? = null,
        /** Tonnage per ISO week, deload weeks marked. */
        val weeklyTonnage: List<WeeklyTonnage> = emptyList(),
        /** Best-ever vs average volume per day type. */
        val dayTypeVolume: List<DayTypeVolumeStats> = emptyList(),

        // ── RECOVER ─────────────────────────────────────────────────────────────────────────────
        val rpeDistribution: List<RpeBucket> = emptyList(),
        val avgRpe: Double? = null,
        /** Average RPE per finished session, oldest → newest. */
        val avgRpePerSession: List<Double> = emptyList(),
        /** Per-week EASY / JUST RIGHT / HARD / BRUTAL counts, last 8 weeks. */
        val weeklyEffort: List<WeeklyEffortCounts> = emptyList(),
        /** Per-day training load for the Banister fitness / fatigue / form curves. */
        val dailyActivity: List<com.forge.app.ui.gym.stats.state.DayLoad> = emptyList()
    )

    /**
     * Single observable feeding the Gym → Stats subtab. Aggregates several DAO flows
     * into the snapshot the UI consumes.
     */
    fun observeGymStats(): Flow<GymStats> {
        return combine(
            loggedSetDao.observeAllFinishedSetsWithSession(),
            loggedExerciseDao.observeRecentPrs()
        ) { allSets, prRows ->
          coroutineScope {
            // Rolling-7-day working sets for the volume-by-muscle read, recomputed per emission so the
            // window slides while the screen stays open. Derived from allSets (already tracked /
            // non-skipped / unassisted) and bucketed by session start.
            val now = clock.nowMs()
            val volumeStartMs = now - WEEK_MS
            val volumeSets = allSets
                .filter { it.sessionStartedAt >= volumeStartMs }
                .map { SetWithExerciseId(it.weightLb, it.reps, it.exerciseId) }
            // Every aggregate query the page needs, fired concurrently rather than in sequence.
            val deloadRowsD = async { sessionDao.allFinishedVolumeDeload() }
            val weekCompD = async { buildWeekComparison() }
            val prDatesD = async { loggedExerciseDao.prDatesPerExercise() }
            val freqD = async { loggedExerciseDao.frequencySince(now - EIGHT_WEEKS_MS) }
            val effortD = async { loggedExerciseDao.effortRatingsSince(now - EIGHT_WEEKS_MS) }
            val dayVolumeD = async { sessionDao.avgMaxVolumeByDayKey() }
            val sessionsD = async { sessionDao.allFinished() }
            val vacationD = async { vacationDao.all() }
            val deloadTrend = buildVolumeDeloadTrend(deloadRowsD.await())
            val prDates = prDatesD.await()
            val lifts = buildE1rmLifts(allSets)
            val sessions = sessionsD.await()
            val sessionStartedAts = sessions.map { it.startedAt }
            GymStats(
                weekComparison = weekCompD.await(),
                overload = buildOverloadSummary(allSets, lifts),

                consistencyStreak = computeConsistencyStreak(
                    sessionStartedAts,
                    com.forge.app.domain.vacation.VacationCalendar.onVacation(vacationD.await())
                ),
                weeklySessionCounts = buildWeeklySessionCounts(sessionStartedAts),
                weeklyDurations = buildWeeklyDurations(sessions),
                exerciseFrequency = buildExerciseFrequency(freqD.await()),

                recentPrs = buildPrEntries(prRows, allSets),
                e1rmLifts = lifts,
                strengthCurves = buildStrengthCurves(allSets),
                prRecency = buildPrRecency(prDates, now),
                timeToPr = buildTimeToPr(prDates),
                repMaxes = buildRepMaxes(allSets),
                patternAxes = buildPatternRadar(allSets, now),

                weeklySetsByMuscle = buildWeeklySetsByMuscle(volumeSets),
                // Always compute the planned targets here (cheap, program-only); freestyle zeroes them in
                // the trailing combine so toggling the mode doesn't re-run any of the heavy aggregations.
                plannedSetsByMuscle =
                    com.forge.app.program.VolumeTargets.plannedWeeklySetsByMuscle(Program.days),
                repRange = buildRepRangeDist(allSets),
                weeklyTonnage = buildWeeklyTonnage(deloadTrend),
                dayTypeVolume = buildDayTypeBestVsAvg(dayVolumeD.await()),

                rpeDistribution = buildRpeDistribution(allSets),
                avgRpe = allSets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average(),
                avgRpePerSession = buildAvgRpePerSession(allSets),
                weeklyEffort = buildEffortDistribution(effortD.await()),
                dailyActivity = buildDailyActivity(allSets)
            )
          }
        }.combine(bodyweightRepo.observeRecent(90)) { stats, bw ->
            // observeRecent is newest-first; reverse to chronological for the relative-strength read.
            stats.copy(bodyweightPoints = bw.reversed().map { BodyweightPoint(it.recordedAt, it.weightLb) })
        }.combine(settingsRepo.freestyleMode) { stats, freestyle ->
            // No fixed plan (freestyle) → no planned volume targets to chart actual sets against. Folded
            // in last so flipping freestyle only re-runs this cheap copy, not the aggregations above.
            if (freestyle) stats.copy(plannedSetsByMuscle = emptyMap()) else stats
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Looks for a session from 1, 3, 6, or 12 months ago (±3-day window). Returns the
     * most distant qualifying match so "1 year ago" takes precedence over "1 month ago". (#106)
     */
    suspend fun findOnThisDayMemory(): OnThisDayMemory? {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (months in listOf(12, 6, 3, 1)) {
            val target = today.minusMonths(months.toLong())
            val targetMs = target.atStartOfDay(zone).toInstant().toEpochMilli()
            val windowMs = 3L * 24 * 60 * 60 * 1000
            val session = sessionDao.sessionNearDate(
                targetMs = targetMs + 12 * 60 * 60 * 1000,
                fromMs = targetMs - windowMs, // ±3 days around the target date (was forward-only)
                toMs = targetMs + windowMs
            ) ?: continue
            val dayName = Program.dayDisplayName(session.dayKey)
            return OnThisDayMemory(
                monthsAgo = months,
                dayName = dayName,
                totalVolumeLb = session.totalVolumeLb ?: 0.0,
                prCount = session.prCount,
                sessionDate = session.startedAt
            )
        }
        return null
    }

    private suspend fun buildWeekComparison(): PeriodComparison? {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val thisWeekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val toMs = { d: LocalDate -> d.atStartOfDay(zone).toInstant().toEpochMilli() }
        val thisW = sessionDao.aggregateInRange(toMs(thisWeekStart), toMs(thisWeekStart.plusWeeks(1)))
        val lastW = sessionDao.aggregateInRange(toMs(lastWeekStart), toMs(thisWeekStart))
        if (lastW.sessionCount == 0 && thisW.sessionCount == 0) return null
        return PeriodComparison(
            label = "WEEK",
            current = PeriodStats(thisW.sessionCount, thisW.totalVolume ?: 0.0, thisW.totalPrs, thisW.totalSets),
            previous = PeriodStats(lastW.sessionCount, lastW.totalVolume ?: 0.0, lastW.totalPrs, lastW.totalSets)
        )
    }

    companion object {
        private const val WEEK_MS: Long = 7L * 24 * 60 * 60 * 1000
        /** The window the exercise-frequency and weekly-effort reads look back over. */
        private const val EIGHT_WEEKS_MS: Long = 8L * WEEK_MS
    }
}
