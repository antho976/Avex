package com.forge.app.ui.gym.stats.state

import com.forge.app.program.MuscleGroup

/** One day row in the "What I did this week" editorial section. */
data class WeekActivityRow(
    val dayOfWeek: Int,            // 0=Mon .. 6=Sun
    val dayLabel: String,          // "MON", "TUE", etc.
    val sessionName: String? = null,
    val muscleWord: String? = null, // "PUSH", "HAMS", etc.
    val durationMin: Int? = null,
    val setCount: Int = 0,
    val hasPr: Boolean = false,
    val cardioType: String? = null, // "Run", "Walk", etc. — null if not cardio
    val cardioDurationMin: Int? = null,
    val cardioDistanceKm: Double? = null
)

/**
 * State for the rebuilt Gym → Stats screen (2026-08-23, "one grammar, four questions").
 *
 * The page cuts by the question a lifter asks, not by data type: SHOW UP / STRONGER / ENOUGH /
 * RECOVER. Every field below renders on every open, at its honest zero — nothing on this screen
 * unlocks, because depth is a property of how a read is worded, not of whether it appears.
 * A beginner reads each section's verdict word; an advanced lifter reads the number beside it.
 *
 * Records and the 26-week consistency heatmap deliberately do NOT live here any more: both move to
 * the Profile (see design/SETTLED.md, 2026-08-23), so the day-detail sheet and CalendarHeatmap
 * components stay in the package unused rather than being deleted.
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    /** True when a stats aggregation threw — the screen shows an error message, not a silent empty. */
    val loadError: Boolean = false,

    // ── Hero ────────────────────────────────────────────────────────────────────────────────────
    /** This ISO week vs last week, side by side — the hero's three figures. */
    val weekComparison: PeriodComparison? = null,
    /** Weekly average of each tracked lift's best e1RM — the hero verdict and its sparkline. */
    val overload: OverloadSummary? = null,

    // ── SHOW UP ─────────────────────────────────────────────────────────────────────────────────
    /** Consecutive recent weeks hitting the session target (vacation weeks bridge, never break). */
    val consistencyStreak: Int = 0,
    /** Sessions per ISO week for the last 12 weeks, oldest → newest. Always 12 entries. */
    val weeklySessionCounts: List<Int> = emptyList(),
    /** Median session length per ISO week, oldest → newest — is a session bloating or shrinking. */
    val weeklyDurations: List<WeeklyDuration> = emptyList(),
    /** What you actually train: distinct sessions per exercise over the last 8 weeks, ranked. */
    val exerciseFrequency: List<ExerciseFrequency> = emptyList(),

    // ── STRONGER ────────────────────────────────────────────────────────────────────────────────
    /** Estimated 1RM per main lift — current value + per-session history. */
    val e1rmLifts: List<E1rmLift> = emptyList(),
    /** PRs on a time axis, folded into the lift each belongs to. */
    val recentPrs: List<PrEntry> = emptyList(),
    /** Per-exercise load-rep scatter + e1RM for the strength curve inside a lift's drill-down. */
    val strengthCurves: List<StrengthCurve> = emptyList(),
    /** The ladder's prescription for a lift that has stopped moving — a chip on that lift's row. */
    val plateauFlags: List<PlateauFlagUi> = emptyList(),
    /** Days since the last PR, overall and per exercise — the drought reading on a lift's row. */
    val prRecency: PrRecency? = null,
    /** Average days between PRs per exercise — that lift's usual pace, for comparison. */
    val timeToPr: List<TimeToPrEntry> = emptyList(),
    /** Dated bodyweight points, oldest → newest — divides e1RM into the relative-strength read. */
    val bodyweightPoints: List<BodyweightPoint> = emptyList(),
    /** User's sex ("male" | "female" | "") — selects the bodyweight-relative strength bands. */
    val userSex: String = "",
    /** Best weight at each rep count on the most-trained lift — pick a working weight from it. */
    val repMaxes: RepMaxSet? = null,
    /** Per movement pattern: recent-window best e1RM against your own all-time peak. */
    val patternAxes: List<PatternAxis> = emptyList(),

    // ── ENOUGH ──────────────────────────────────────────────────────────────────────────────────
    /** Working sets per muscle this ISO week. */
    val weeklySetsByMuscle: List<MuscleSetCount> = emptyList(),
    /** Planned weekly sets per muscle from the active program (empty in freestyle). */
    val plannedSetsByMuscle: Map<MuscleGroup, Int> = emptyMap(),
    /** How every logged set splits across strength / hypertrophy / endurance rep ranges. */
    val repRange: RepRangeDist? = null,
    /** Always-on push/pull + quad/ham balance bars (System 4 counting). */
    val balanceRatios: List<BalanceRatioUi> = emptyList(),
    /** Tonnage per ISO week, deload weeks marked. */
    val weeklyTonnage: List<WeeklyTonnage> = emptyList(),
    /** Best-ever vs average volume per day type — which session you coast through. */
    val dayTypeVolume: List<DayTypeVolumeStats> = emptyList(),

    // ── RECOVER ─────────────────────────────────────────────────────────────────────────────────
    /** Fatigue pulse (System 5). Null until the engine read lands or while data gates fail. */
    val readinessPulse: ReadinessPulse? = null,
    /** The learned deload score threshold — the marked gate on the fatigue meter. */
    val readinessThreshold: Int? = null,
    /** Count of sets at each RPE value + the overall average. */
    val rpeDistribution: List<RpeBucket> = emptyList(),
    val avgRpe: Double? = null,
    /** Average RPE per finished session, oldest → newest — is effort drifting up. */
    val avgRpePerSession: List<Double> = emptyList(),
    /** Per-week EASY / JUST RIGHT / HARD / BRUTAL counts for the last 8 weeks. */
    val weeklyEffort: List<WeeklyEffortCounts> = emptyList(),
    /** Per-day training load — feeds the Banister fitness / fatigue / form curves. */
    val dailyActivity: List<DayLoad> = emptyList()
)

/** One ISO week's total tonnage for the Volume trend bars. */
data class WeeklyTonnage(
    val weekLabel: String,
    val volumeLb: Double,
    val isDeload: Boolean
)

/** Sessions per day of week (Mon..Sun) + the sets-weighted best training hour. */
data class TrainingTimes(
    val sessionsByDayOfWeek: List<Int>,
    /** e.g. "evening (18:00)" — null until enough sets accrue. */
    val bestHourLabel: String?
)

/** Median session length for one ISO week. */
data class WeeklyDuration(
    val weekLabel: String,
    val medianMin: Int
)

/** One dated bodyweight entry for the Body trend line. */
data class BodyweightPoint(
    val recordedAt: Long,
    val weightLb: Double
)

/** Weekly average of per-lift best e1RM across the tracked lifts, oldest → newest. */
data class OverloadSummary(
    /** Last week-with-data's average e1RM. */
    val current: Double,
    /** Avg e1RM per ISO week that had data, oldest → newest (≤ 12 weeks). */
    val weekly: List<Double>
) {
    val prevWeek: Double? get() = if (weekly.size >= 2) weekly[weekly.size - 2] else null
    val deltaVsPrevWeek: Double? get() = prevWeek?.let { current - it }
}

/** Days since the last PR — overall and per exercise. */
data class PrRecency(
    val daysSinceLast: Int,
    val byExercise: Map<String, Int>
)

/** One axis of the movement-pattern radar: recent-window best vs all-time best e1RM. */
data class PatternAxis(
    val label: String,
    val currentE1rm: Double,
    val peakE1rm: Double
) {
    /** Current shape vs your own peak — the only honest cross-pattern comparison. */
    val fraction: Double get() = if (peakE1rm > 0) (currentE1rm / peakE1rm).coerceIn(0.0, 1.0) else 0.0
}

/** Working-set count for one muscle group in the current week (Phase 2). */
data class MuscleSetCount(val muscle: MuscleGroup, val sets: Int) {
    /** Rudimentary landmark guidance: under ~10 = low, over ~20 = high. */
    val low: Int get() = 10
    val high: Int get() = 20
}

/** How logged sets split across rep ranges (Phase 2). */
data class RepRangeDist(val strength: Int, val hypertrophy: Int, val endurance: Int) {
    val total: Int get() = strength + hypertrophy + endurance
}

/** Number of sets logged at a given RPE value (Phase 3). */
data class RpeBucket(val rpe: Double, val count: Int)

/** Estimated 1RM progression for one lift (Epley: w × (1 + reps/30)). */
data class E1rmLift(
    val exerciseId: String,
    val exerciseName: String,
    val currentE1rm: Double,
    /** Best e1RM per session, oldest → newest. */
    val history: List<Double>,
    /** Average growth per month, as a percent. Null if <2 sessions. */
    val monthlyPct: Double? = null,
    /** True when the last few sessions have been flat (no meaningful progress). */
    val stalling: Boolean = false
) {
    /** Change from first recorded e1RM to current. */
    val delta: Double get() = if (history.size >= 2) currentE1rm - history.first() else 0.0
}

/** Best weight achieved at a given rep count. */
data class RepMaxEntry(val reps: Int, val weightLb: Double)

/** Rep-max table for a single exercise. */
data class RepMaxSet(val exerciseName: String, val entries: List<RepMaxEntry>)

/** Per-muscle weekly volume, sorted descending by volume in the repository. */
data class MuscleVolume(
    val muscle: MuscleGroup,
    val volumeLb: Double
)

data class PrEntry(
    val date: Long,
    val exerciseName: String,
    val weightLb: Double,
    val reps: Int
)

/** All-time best set per exercise, shown in the Records hall-of-fame (#14). */
data class PrRecord(
    val exerciseId: String,
    val exerciseName: String,
    val maxWeightLb: Double,
    val bestReps: Int,
    val sessionDate: Long,
    val muscle: MuscleGroup,
    /** Relative strength as multiple of bodyweight. Null if no bodyweight logged (#77). */
    val relativeStrength: Double? = null
)

/** One data point in the per-exercise weight history (#27). */
data class HistoryPoint(
    val sessionDate: Long,
    val maxWeightLb: Double
)

/** How many sessions in past 8 weeks included a given exercise (#73). */
data class ExerciseFrequency(
    val exerciseId: String,
    val exerciseName: String,
    val sessionCount: Int,
    val outOf: Int
)

/** Average days between consecutive PRs for an exercise (#74). */
data class TimeToPrEntry(
    val exerciseId: String,
    val exerciseName: String,
    val avgDaysBetween: Int,
    val prCount: Int
)

/** EASY/JUST_RIGHT/HARD/BRUTAL counts for a single ISO-week (#75). */
data class WeeklyEffortCounts(
    val weekLabel: String,
    val easy: Int,
    val justRight: Int,
    val hard: Int,
    val brutal: Int
) {
    val total: Int get() = easy + justRight + hard + brutal
}

/** One point on the volume trend chart, with a deload marker (#126). */
data class VolumeDeloadPoint(
    val sessionDate: Long,
    val dayKey: String,
    val totalVolumeLb: Double,
    val isDeload: Boolean
)

/** Best-ever vs average volume for a day type (#132). */
data class DayTypeVolumeStats(
    val dayKey: String,
    val dayName: String,
    val avgVolumeLb: Double,
    val maxVolumeLb: Double,
    val sessionCount: Int
)

/** Stats for a time window (week or month) used for period-over-period comparison (#34, #130). */
data class PeriodStats(
    val sessions: Int,
    val volumeLb: Double,
    val prs: Int,
    val sets: Int
)

/** Side-by-side comparison of two consecutive periods (#34 week, #130 month). */
data class PeriodComparison(
    val label: String,
    val current: PeriodStats,
    val previous: PeriodStats
) {
    val volumeDelta: Double get() = current.volumeLb - previous.volumeLb
    val sessionsDelta: Int get() = current.sessions - previous.sessions
    val prsDelta: Int get() = current.prs - previous.prs
}

/** Session efficiency / lifetime metrics row (#40). */
data class LifetimeMetrics(
    val lifetimeVolumeLb: Double,
    val totalSessions: Int,
    val avgSessionVolumeLb: Double,
    val avgSetCount: Double
)
