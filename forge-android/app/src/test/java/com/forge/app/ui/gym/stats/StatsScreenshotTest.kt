package com.forge.app.ui.gym.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.forge.app.program.MuscleGroup
import com.forge.app.ui.gym.stats.state.BalanceRatioUi
import com.forge.app.ui.gym.stats.state.BodyweightPoint
import com.forge.app.ui.gym.stats.state.DayLoad
import com.forge.app.ui.gym.stats.state.DayTypeVolumeStats
import com.forge.app.ui.gym.stats.state.E1rmLift
import com.forge.app.ui.gym.stats.state.ExerciseFrequency
import com.forge.app.ui.gym.stats.state.MuscleSetCount
import com.forge.app.ui.gym.stats.state.OverloadSummary
import com.forge.app.ui.gym.stats.state.PatternAxis
import com.forge.app.ui.gym.stats.state.PeriodComparison
import com.forge.app.ui.gym.stats.state.PeriodStats
import com.forge.app.ui.gym.stats.state.PlateauFlagUi
import com.forge.app.ui.gym.stats.state.PrEntry
import com.forge.app.ui.gym.stats.state.PrRecency
import com.forge.app.ui.gym.stats.state.PulseBand
import com.forge.app.ui.gym.stats.state.ReadinessPulse
import com.forge.app.ui.gym.stats.state.RepMaxEntry
import com.forge.app.ui.gym.stats.state.RepMaxSet
import com.forge.app.ui.gym.stats.state.RepRangeDist
import com.forge.app.ui.gym.stats.state.RepWeightPoint
import com.forge.app.ui.gym.stats.state.RpeBucket
import com.forge.app.ui.gym.stats.state.StatsUiState
import com.forge.app.ui.gym.stats.state.StrengthCurve
import com.forge.app.ui.gym.stats.state.TimeToPrEntry
import com.forge.app.ui.gym.stats.state.WeeklyDuration
import com.forge.app.ui.gym.stats.state.WeeklyEffortCounts
import com.forge.app.ui.gym.stats.state.WeeklyTonnage
import com.forge.app.ui.theme.ForgeMotion
import com.forge.app.ui.theme.ForgeTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Golden screenshots of the rebuilt Gym → Stats page (DESIGN §3 overview archetype, §14 physics).
 *
 * Stats is now the most instrument-dense screen in the app: sixteen sections across four lenses,
 * every one of which renders at every level of history. `DesignDoctrineTest` can tell you an alpha
 * is off-ladder; only a rendered pixel can tell you a verdict line collides with its mark at 200%
 * font scale, or that a zero state reads as damage rather than as data. Both are pinned here.
 *
 * Record or re-record:  gradle -p forge-android :app:recordRoborazziDebug
 * Verify (what CI runs): gradle -p forge-android :app:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class StatsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @After fun restoreMotion() { ForgeMotion.durationScale = 1f }

    /** Matches RecipeScreenshotTest: 0.1% absorbs font-rasterisation jitter, not layout change. */
    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.001f)
    )

    private fun shoot(
        name: String,
        state: StatsUiState = LOADED,
        lens: StatsLens,
        fontScale: Float = 1f,
        amoled: Boolean = false,
        accentEnabled: Boolean = true
    ) {
        // Capture in the reduce-motion ground (§9): every entrance and draw-in settles instantly.
        // A LazyColumn item below the fold composes DURING the capture pass, so advancing the clock
        // beforehand cannot settle it — it would render faint and offset, reading as a layout bug
        // that was never there. This also means these goldens double as the reduce-motion check.
        ForgeMotion.durationScale = 0f
        compose.setContent {
            ForgeTheme(amoledMode = amoled, accentEnabled = accentEnabled) {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = fontScale
                    )
                ) {
                    StatsPage(state = state, lens = lens, onLens = {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", options)
    }

    // ── With a real history behind it ───────────────────────────────────────────────────────────
    @Test fun showUp() = shoot("stats-show-up", lens = StatsLens.SHOW_UP)
    @Test fun stronger() = shoot("stats-stronger", lens = StatsLens.STRONGER)
    @Test fun enough() = shoot("stats-enough", lens = StatsLens.ENOUGH)
    @Test fun recover() = shoot("stats-recover", lens = StatsLens.RECOVER)

    // ── At zero: the page's whole argument is that this still reads as data ─────────────────────
    @Test fun showUpAtZero() = shoot("stats-show-up-zero", ZERO, StatsLens.SHOW_UP)
    @Test fun strongerAtZero() = shoot("stats-stronger-zero", ZERO, StatsLens.STRONGER)
    @Test fun enoughAtZero() = shoot("stats-enough-zero", ZERO, StatsLens.ENOUGH)

    /**
     * The state a real first-run account is actually in: a generated plan, nothing logged against
     * it yet. Distinct from ZERO, and the one that was wrong on device — every muscle rendered as a
     * failed target on day one.
     */
    @Test fun enoughPlanUntrained() = shoot("stats-enough-untrained", PLAN_UNTRAINED, StatsLens.ENOUGH)
    @Test fun recoverAtZero() = shoot("stats-recover-zero", ZERO, StatsLens.RECOVER)

    // ── §14: the screen must survive the biggest font ──────────────────────────────────────────
    @Test fun strongerLarge() = shoot("stats-stronger-200", lens = StatsLens.STRONGER, fontScale = 2f)
    @Test fun enoughLarge() = shoot("stats-enough-200", lens = StatsLens.ENOUGH, fontScale = 2f)
    @Test fun recoverLarge() = shoot("stats-recover-200", lens = StatsLens.RECOVER, fontScale = 2f)


    // ── Shipped grounds, not variants ──────────────────────────────────────────────────────────
    @Test fun strongerAmoled() = shoot("stats-stronger-amoled", lens = StatsLens.STRONGER, amoled = true)
    @Test fun enoughMono() =
        shoot("stats-enough-mono", lens = StatsLens.ENOUGH, accentEnabled = false)

    private companion object {
        /** Nothing logged: every section falls to the state it draws before any history exists. */
        val ZERO = StatsUiState(isLoading = false, weeklySessionCounts = List(12) { 0 })

        /** A generated plan with no work logged against it yet — a first-run account, day one. */
        val PLAN_UNTRAINED = ZERO.copy(
            plannedSetsByMuscle = mapOf(
                MuscleGroup.CHEST to 10, MuscleGroup.BACK to 14, MuscleGroup.SHOULDERS to 10,
                MuscleGroup.REAR_DELTS to 6, MuscleGroup.BICEPS to 12, MuscleGroup.TRICEPS to 13,
                MuscleGroup.QUADS to 10, MuscleGroup.HAMSTRINGS to 10, MuscleGroup.GLUTES to 4,
                MuscleGroup.CALVES to 4, MuscleGroup.CORE to 4
            )
        )

        /**
         * A believable intermediate lifter: fourteen weeks in, four lifts moving and one stalled,
         * behind on one muscle, fatigue building. Synthetic demonstration data, never shipped.
         */
        val LOADED: StatsUiState = run {
            val today = LocalDate.now().toEpochDay()
            StatsUiState(
                isLoading = false,
                weekComparison = PeriodComparison(
                    label = "WEEK",
                    current = PeriodStats(sessions = 4, volumeLb = 41250.0, prs = 2, sets = 68),
                    previous = PeriodStats(sessions = 3, volumeLb = 37900.0, prs = 1, sets = 59)
                ),
                overload = OverloadSummary(
                    current = 214.0,
                    weekly = listOf(
                        186.0, 189.0, 188.0, 194.0, 197.0, 196.0,
                        201.0, 204.0, 203.0, 208.0, 211.0, 214.0
                    )
                ),
                consistencyStreak = 6,
                weeklySessionCounts = listOf(2, 3, 3, 1, 3, 4, 3, 3, 4, 3, 4, 4),
                weeklyDurations = (0..11).map {
                    WeeklyDuration("0${(it % 9) + 1}-1${it % 8}", 54 + (it % 5) * 4)
                },
                exerciseFrequency = listOf(
                    ExerciseFrequency("bench", "Bench press", 14, 14),
                    ExerciseFrequency("squat", "Back squat", 12, 14),
                    ExerciseFrequency("row", "Barbell row", 11, 14),
                    ExerciseFrequency("ohp", "Overhead press", 8, 14),
                    ExerciseFrequency("dl", "Deadlift", 6, 14),
                    ExerciseFrequency("curl", "Barbell curl", 5, 14)
                ),
                e1rmLifts = listOf(
                    E1rmLift("dl", "Deadlift", 405.0, listOf(340.0, 355.0, 370.0, 385.0, 405.0), 6.4),
                    E1rmLift("squat", "Back squat", 315.0, listOf(265.0, 275.0, 290.0, 300.0, 315.0), 5.1),
                    E1rmLift("bench", "Bench press", 225.0, listOf(190.0, 198.0, 205.0, 215.0, 225.0), 4.2),
                    E1rmLift("row", "Barbell row", 185.0, listOf(160.0, 168.0, 176.0, 182.0, 185.0), 2.7),
                    E1rmLift("ohp", "Overhead press", 135.0, listOf(132.0, 133.0, 134.0, 135.0, 135.0), 0.4, stalling = true)
                ),
                recentPrs = listOf(
                    PrEntry(System.currentTimeMillis() - 3 * 86_400_000L, "Deadlift", 385.0, 3),
                    PrEntry(System.currentTimeMillis() - 12 * 86_400_000L, "Back squat", 285.0, 5)
                ),
                strengthCurves = listOf(
                    StrengthCurve(
                        "dl", "Deadlift",
                        listOf(
                            RepWeightPoint(1, 385.0), RepWeightPoint(3, 355.0), RepWeightPoint(3, 345.0),
                            RepWeightPoint(5, 325.0), RepWeightPoint(5, 315.0), RepWeightPoint(8, 285.0),
                            RepWeightPoint(10, 265.0)
                        ),
                        405.0
                    )
                ),
                plateauFlags = listOf(
                    PlateauFlagUi(
                        "ohp", "Overhead press", "SHIFT TO 8-10 REPS",
                        "Four sessions inside 1% of the same estimated max. A rep-range change gives it somewhere to move."
                    )
                ),
                prRecency = PrRecency(3, mapOf("dl" to 3, "squat" to 12, "bench" to 21, "ohp" to 64)),
                timeToPr = listOf(
                    TimeToPrEntry("dl", "Deadlift", 16, 5),
                    TimeToPrEntry("ohp", "Overhead press", 31, 3)
                ),
                bodyweightPoints = listOf(BodyweightPoint(System.currentTimeMillis(), 178.0)),
                userSex = "male",
                repMaxes = RepMaxSet(
                    "Bench press",
                    listOf(
                        RepMaxEntry(1, 215.0), RepMaxEntry(3, 200.0), RepMaxEntry(5, 185.0),
                        RepMaxEntry(8, 170.0), RepMaxEntry(10, 155.0), RepMaxEntry(12, 145.0)
                    )
                ),
                patternAxes = listOf(
                    PatternAxis("PUSH", 225.0, 225.0),
                    PatternAxis("PULL", 185.0, 195.0),
                    PatternAxis("QUADS", 315.0, 315.0),
                    PatternAxis("POSTERIOR", 340.0, 405.0),
                    PatternAxis("CORE", 90.0, 110.0)
                ),
                weeklySetsByMuscle = listOf(
                    MuscleSetCount(MuscleGroup.CHEST, 14),
                    MuscleSetCount(MuscleGroup.BACK, 16),
                    MuscleSetCount(MuscleGroup.QUADS, 12),
                    MuscleSetCount(MuscleGroup.HAMSTRINGS, 6),
                    MuscleSetCount(MuscleGroup.SHOULDERS, 9)
                ),
                plannedSetsByMuscle = mapOf(
                    MuscleGroup.CHEST to 14,
                    MuscleGroup.BACK to 16,
                    MuscleGroup.QUADS to 14,
                    MuscleGroup.HAMSTRINGS to 10,
                    MuscleGroup.SHOULDERS to 8
                ),
                repRange = RepRangeDist(strength = 96, hypertrophy = 412, endurance = 74),
                balanceRatios = listOf(
                    BalanceRatioUi("Push / Pull", "Push", "Pull", 23, 25, 0.7, 1.3),
                    BalanceRatioUi("Quad / Ham", "Quad", "Ham", 12, 6, 0.7, 1.3)
                ),
                weeklyTonnage = (0..11).map {
                    WeeklyTonnage(
                        "0${(it % 9) + 1}-1${it % 8}",
                        32000.0 + it * 900 - (if (it == 7) 12000 else 0),
                        isDeload = it == 7
                    )
                },
                dayTypeVolume = listOf(
                    DayTypeVolumeStats("upper_a", "Upper A", 11200.0, 13400.0, 14),
                    DayTypeVolumeStats("lower_a", "Lower A", 14800.0, 16100.0, 13),
                    DayTypeVolumeStats("upper_b", "Upper B", 9100.0, 13900.0, 12),
                    DayTypeVolumeStats("lower_b", "Lower B", 13200.0, 15000.0, 12)
                ),
                readinessPulse = ReadinessPulse(
                    band = PulseBand.BUILDING,
                    score = 32,
                    drivers = listOf("4 hard sessions in 7 days", "Sleep 6.4h average", "2 sets rated brutal")
                ),
                readinessThreshold = 34,
                rpeDistribution = listOf(
                    RpeBucket(6.0, 18), RpeBucket(6.5, 24), RpeBucket(7.0, 61),
                    RpeBucket(7.5, 88), RpeBucket(8.0, 104), RpeBucket(8.5, 71),
                    RpeBucket(9.0, 42), RpeBucket(9.5, 14), RpeBucket(10.0, 6)
                ),
                avgRpe = 7.9,
                avgRpePerSession = listOf(
                    7.2, 7.4, 7.3, 7.6, 7.5, 7.8, 7.7, 8.0, 8.1, 8.0, 8.2, 8.3
                ),
                weeklyEffort = (0..7).map {
                    WeeklyEffortCounts(
                        "0${(it % 9) + 1}-1${it % 8}",
                        easy = 3, justRight = 8 - (it % 3), hard = 4 + (it % 4), brutal = it % 3
                    )
                },
                dailyActivity = (0..120).mapNotNull { back ->
                    if (back % 7 == 0 || back % 7 == 3) null
                    else DayLoad(today - back, sets = 14 + back % 5, volumeLb = 9000.0 + (back % 11) * 400)
                }
            )
        }
    }
}
