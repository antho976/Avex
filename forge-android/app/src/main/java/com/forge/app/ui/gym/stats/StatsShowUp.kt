package com.forge.app.ui.gym.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.forge.app.ui.gym.stats.components.RowMark
import com.forge.app.ui.gym.stats.components.StatsRow
import com.forge.app.ui.gym.stats.state.ExerciseFrequency

/**
 * The sessions-per-week the consistency streak is counted against. Mirrors CONSISTENCY_TARGET in
 * StatsEffortAggregations, so the rows and the streak can never disagree about what "a week that
 * counts" means.
 */
private const val SESSION_TARGET_PER_WEEK = 3

/** How many exercises the "what you train" ranking shows before it stops being a ranking. */
private const val FREQUENCY_ROWS = 5

// ── SHOW UP — did I train, and how reliably ─────────────────────────────────────────────────────
//
// The twelve-week bar rail that used to anchor this lens is gone: it was a third mark shape, and
// the two readings it carried say the same thing on the shared row grid. The session-length trend
// went with it (no decision attached). The heatmap and its day sheet moved to the Profile.

/** Cadence as two readings on the grid: the twelve-week hit rate, and the week in progress. */
@Composable
internal fun ColumnScope.CadenceContent(weeklySessions: List<Int>, c: StatsColors) {
    val weeks = weeklySessions.size.coerceAtLeast(1)
    val hits = weeklySessions.count { it >= SESSION_TARGET_PER_WEEK }
    val thisWeek = weeklySessions.lastOrNull() ?: 0
    Column(Modifier.fillMaxWidth()) {
        StatsRow(
            label = "Last $weeks weeks",
            value = "$hits of $weeks",
            mark = RowMark.Meter(fill = hits.toFloat() / weeks),
            c = c.row,
            contentDescription =
                "$hits of the last $weeks weeks reached $SESSION_TARGET_PER_WEEK sessions"
        )
        StatsRow(
            label = "This week",
            value = "$thisWeek of $SESSION_TARGET_PER_WEEK",
            mark = RowMark.Meter(fill = thisWeek.toFloat() / SESSION_TARGET_PER_WEEK),
            c = c.row,
            contentDescription = "$thisWeek of $SESSION_TARGET_PER_WEEK sessions this week"
        )
    }
}

/** The cadence verdict — a few words, never a sentence. */
internal fun cadenceRead(streakWeeks: Int, weeklySessions: List<Int>): String = when {
    weeklySessions.all { it == 0 } -> "Not started"
    streakWeeks >= 1 -> "$streakWeeks-week run"
    else -> "Run broken"
}

/** What you actually train, ranked by how many recent sessions included it. */
@Composable
internal fun ColumnScope.ExerciseFrequencyContent(frequency: List<ExerciseFrequency>, c: StatsColors) {
    val shown = frequency.take(FREQUENCY_ROWS)
    if (shown.isEmpty()) {
        StatsRow("Sessions logged", "0 of 1", RowMark.Meter(fill = 0f), c.row)
        return
    }
    val ceiling = shown.first().sessionCount.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth()) {
        shown.forEach { row ->
            StatsRow(
                label = row.exerciseName,
                value = "${row.sessionCount}",
                mark = RowMark.Meter(fill = row.sessionCount.toFloat() / ceiling),
                c = c.row,
                contentDescription = "${row.exerciseName}, ${row.sessionCount} sessions"
            )
        }
    }
}

/** The exercise-frequency verdict. */
internal fun exerciseFrequencyRead(frequency: List<ExerciseFrequency>): String =
    frequency.firstOrNull()?.let { "top: ${it.exerciseName}" } ?: "Nothing logged"
