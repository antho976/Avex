package com.forge.app.ui.gym.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.forge.app.ui.gym.stats.components.RowMark
import com.forge.app.ui.gym.stats.components.StatsRow
import com.forge.app.ui.gym.stats.state.PulseBand
import com.forge.app.ui.gym.stats.state.ReadinessPulse
import com.forge.app.ui.gym.stats.state.RpeBucket
import com.forge.app.ui.gym.stats.state.WeeklyEffortCounts
import com.forge.app.ui.theme.ForgeError
import com.forge.app.ui.theme.ForgeSuccess
import com.forge.app.ui.theme.ForgeWarning

/** How far above the deload threshold the fatigue scale runs, so the top band stays visible. */
private const val FATIGUE_SCALE_HEADROOM = 1.5f

/** The threshold the scale draws against before the engine has learned one. Empty rows only. */
private const val DEFAULT_DELOAD_THRESHOLD = 34

/** RPE at or above this counts as a hard set. */
private const val HARD_RPE = 8.0

/** The effort scale's ends, so a rating reads as a position on a known range. */
private const val RPE_FLOOR = 5.0
private const val RPE_CEILING = 10.0

/** How many recent sessions the effort-drift reading counts as "lately". */
private const val RPE_DRIFT_WINDOW = 3

// ── RECOVER — can I take more ───────────────────────────────────────────────────────────────────
//
// Two sections. The weekly hard-or-brutal rail was cut (it restated the effort rows from a second
// self-report), and so was the Banister fitness/fatigue chart: it carried no decision by its own
// admission, and it was a whole extra chart shape on a page that needed fewer.

/** The engine's fatigue score against the threshold it learned, as one banded row. */
@Composable
internal fun ColumnScope.FatigueContent(pulse: ReadinessPulse?, threshold: Int?, c: StatsColors) {
    val gate = threshold ?: DEFAULT_DELOAD_THRESHOLD
    val scaleMax = (gate * FATIGUE_SCALE_HEADROOM).coerceAtLeast(1f)
    Column(Modifier.fillMaxWidth()) {
        StatsRow(
            label = "Fatigue",
            value = if (pulse == null) "0 of $gate" else "${pulse.score} of $gate",
            // The zones are the engine's own: fresh, the two points before the gate, and past it.
            mark = RowMark.Banded(
                marker = pulse?.let { it.score / scaleMax },
                edges = listOf((gate - 2) / scaleMax, gate / scaleMax),
                // §5's reserved true-state colors: past the gate is a state to act on, not a bar
                // that happens to be full. The accent ramp would read as "further along is better".
                zoneColors = listOf(ForgeSuccess, ForgeWarning, ForgeError)
            ),
            c = c.row,
            contentDescription = if (pulse == null) {
                "The fatigue scale, with no score on it yet"
            } else {
                "Fatigue ${pulse.score} against a deload threshold of $gate, ${pulse.band.label}"
            }
        )
    }
}

/** The fatigue verdict. */
internal fun fatigueRead(pulse: ReadinessPulse?): String = when (pulse?.band) {
    null -> "No read yet"
    PulseBand.FRESH -> "Room to push"
    PulseBand.BUILDING -> "Load building"
    PulseBand.DELOAD_SOON -> "Deload close"
}

/**
 * Effort as three readings rather than a nine-bar histogram: where your sets sit on the scale, how
 * much of the work is genuinely hard, and whether that is drifting. The histogram said the same
 * thing in a shape nothing else on the page used.
 */
@Composable
internal fun ColumnScope.EffortContent(
    buckets: List<RpeBucket>,
    avgRpe: Double?,
    perSession: List<Double>,
    c: StatsColors
) {
    val totalSets = buckets.sumOf { it.count }
    val hardSets = buckets.filter { it.rpe >= HARD_RPE }.sumOf { it.count }
    val hardPct = if (totalSets > 0) hardSets * 100 / totalSets else 0
    val lately = perSession.takeLast(RPE_DRIFT_WINDOW).takeIf { it.isNotEmpty() }?.average()
    // A rating means nothing as a raw fraction of ten, so every effort row is placed on the 5-to-10
    // range a working set actually lives in.
    val onScale = { v: Double -> ((v - RPE_FLOOR) / (RPE_CEILING - RPE_FLOOR)).toFloat() }
    Column(Modifier.fillMaxWidth()) {
        StatsRow(
            label = "Typical set",
            value = avgRpe?.let { "%.1f".format(it) } ?: "0",
            mark = RowMark.Meter(fill = avgRpe?.let(onScale) ?: 0f),
            c = c.row,
            contentDescription = avgRpe?.let { "Average rated effort %.1f out of 10".format(it) }
                ?: "No sets rated yet"
        )
        StatsRow(
            label = "Hard sets",
            value = "$hardPct%",
            mark = RowMark.Meter(fill = hardPct / 100f),
            c = c.row,
            contentDescription = "$hardPct percent of rated sets were 8 or above"
        )
        StatsRow(
            label = "Lately",
            value = lately?.let { "%.1f".format(it) } ?: "0",
            mark = RowMark.Meter(fill = lately?.let(onScale) ?: 0f),
            c = c.row,
            contentDescription = lately?.let {
                "Your last $RPE_DRIFT_WINDOW sessions averaged %.1f".format(it)
            } ?: "Not enough sessions to compare yet"
        )
    }
}

/** The effort verdict, carrying the drift when there is enough history to see one. */
internal fun effortRead(avgRpe: Double?, perSession: List<Double>): String {
    if (avgRpe == null) return "Nothing rated"
    if (perSession.size > RPE_DRIFT_WINDOW) {
        val lately = perSession.takeLast(RPE_DRIFT_WINDOW).average()
        val before = perSession.dropLast(RPE_DRIFT_WINDOW).average()
        if (lately - before >= 0.3) return "Climbing"
        if (lately - before <= -0.3) return "Easing"
    }
    return when {
        avgRpe >= 8.5 -> "Near your limit"
        avgRpe >= 7.0 -> "Working hard"
        else -> "Reps in reserve"
    }
}

/**
 * How the work FELT, from your own per-exercise rating. Distinct from the numeric effort rows above:
 * that is per set and scored, this is per exercise and chosen, and the two disagree often enough to
 * be worth both.
 */
@Composable
internal fun ColumnScope.EffortMixContent(weeks: List<WeeklyEffortCounts>, c: StatsColors) {
    val rated = weeks.filter { it.total > 0 }
    val total = rated.sumOf { it.total }.coerceAtLeast(1)
    val bands = listOf(
        "Easy" to rated.sumOf { it.easy },
        "Just right" to rated.sumOf { it.justRight },
        "Hard" to rated.sumOf { it.hard },
        "Brutal" to rated.sumOf { it.brutal }
    )
    Column(Modifier.fillMaxWidth()) {
        bands.forEach { (label, count) ->
            val pct = if (rated.isEmpty()) 0 else count * 100 / total
            StatsRow(
                label = label,
                value = "$pct%",
                mark = RowMark.Meter(fill = count.toFloat() / total),
                c = c.row,
                contentDescription = "$label, $count exercises, $pct percent"
            )
        }
    }
}

/** The effort-mix verdict — the band the most exercises landed in. */
internal fun effortMixRead(weeks: List<WeeklyEffortCounts>): String {
    val rated = weeks.filter { it.total > 0 }
    if (rated.isEmpty()) return "Nothing rated"
    val bands = listOf(
        "mostly easy" to rated.sumOf { it.easy },
        "mostly right" to rated.sumOf { it.justRight },
        "mostly hard" to rated.sumOf { it.hard },
        "mostly brutal" to rated.sumOf { it.brutal }
    )
    return bands.maxBy { it.second }.first
}
