package com.forge.app.ui.gym.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.forge.app.ui.common.EditorialFigure
import com.forge.app.ui.common.rememberDrawProgress
import com.forge.app.ui.gym.stats.components.Sparkline
import com.forge.app.ui.gym.stats.state.OverloadSummary
import com.forge.app.ui.gym.stats.state.StatsUiState
import com.forge.app.ui.theme.ForgeMotion
import com.forge.app.ui.theme.MonoSectionAnchor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** How far back the momentum read looks when the history allows it. Four weeks ≈ one month. */
private const val MOMENTUM_LOOKBACK_WEEKS = 4
/** Below this monthly percentage the trend is holding, not moving. Noise, not a direction. */
private const val MOMENTUM_DEADBAND_PCT = 1.0

/**
 * The Stats hero: the page's single most important answer, which on a training log is not what you
 * did but whether the needle is moving.
 *
 * `STATS · WEEK OF AUG 18` → one serif verdict → the reading behind it → the twelve-week shape →
 * this week's three counts. A lifter in their second week reads the serif word and stops; a lifter
 * in their sixth year reads the percentage, the lift count and the slope. Neither is shown a
 * placeholder where the other sees content, and below a gate the reading says what still has to
 * happen rather than "not enough data" (§4.9).
 */
@Composable
internal fun ColumnScope.StatsHeroContent(state: StatsUiState, c: StatsColors) {
    val today = remember { LocalDate.now() }
    val weekStart = remember(today) { today.minusDays(today.dayOfWeek.value.toLong() - 1) }
    val weekLabel = remember(weekStart) {
        weekStart.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())).uppercase()
    }
    val momentum = remember(state.overload) { momentumOf(state.overload, state.e1rmLifts.size) }

    // §3's mono eyebrow: identity + human date. The screen names itself here, so the hosting top
    // bar carries no title (§4.6).
    Text(
        "STATS · WEEK OF $weekLabel",
        style = MonoSectionAnchor,
        color = c.muted,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(Modifier.height(10.dp))
    // The one serif line on the page. It states a result, so it earns the voice (§3/§11) — and it
    // takes no terminal period.
    Text(momentum.verdict, style = MaterialTheme.typography.headlineMedium, color = c.onBg)
    Spacer(Modifier.height(8.dp))
    Text(momentum.reading, style = MaterialTheme.typography.bodySmall, color = c.muted)

    // The shape behind the verdict. A single point would be a flat line reading as broken, so the
    // mark appears from two weeks on and the figures below carry the zero state until then (§12).
    val weekly = state.overload?.weekly.orEmpty()
    if (weekly.size >= 2) {
        Spacer(Modifier.height(16.dp))
        val progress = rememberDrawProgress(weekly.size, ForgeMotion.drawTween())
        Sparkline(
            values = weekly,
            lineColor = c.accent,
            trendColor = c.muted,
            minValue = weekly.min(),
            maxValue = weekly.max(),
            progress = progress,
            showStartEndpoint = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(STATS_RAIL_H)
                .semantics {
                    contentDescription =
                        "Average estimated one-rep max across your tracked lifts, " +
                            "${weekly.size} weeks. ${momentum.verdict}. ${momentum.reading}"
                }
        )
    }

    Spacer(Modifier.height(20.dp))
    WeekFigures(state, c)
}

/** This week's three counts, each with its change against last week. Honest zeros, never dashes. */
@Composable
private fun WeekFigures(state: StatsUiState, c: StatsColors) {
    val cmp = state.weekComparison
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        EditorialFigure(
            value = "${cmp?.current?.sessions ?: 0}",
            label = "sessions",
            onBg = c.onBg, muted = c.muted, accent = c.accent,
            delta = cmp?.sessionsDelta,
            modifier = Modifier.weight(1f)
        )
        EditorialFigure(
            value = "${cmp?.current?.sets ?: 0}",
            label = "sets",
            onBg = c.onBg, muted = c.muted, accent = c.accent,
            delta = cmp?.let { it.current.sets - it.previous.sets },
            modifier = Modifier.weight(1f)
        )
        EditorialFigure(
            value = "${cmp?.current?.prs ?: 0}",
            label = "prs",
            onBg = c.onBg, muted = c.muted, accent = c.accent,
            delta = cmp?.prsDelta,
            modifier = Modifier.weight(1f)
        )
    }
}

/** The hero's verdict word and the reading it came from. */
internal data class Momentum(val verdict: String, val reading: String)

/**
 * Momentum from the weekly average of every tracked lift's best estimated 1RM: the current week
 * against roughly a month back, expressed per month so the number means the same thing whatever
 * span it was measured over.
 *
 * Each state below a real trend says what still has to happen, never "not enough data" (§4.9).
 */
internal fun momentumOf(overload: OverloadSummary?, liftCount: Int): Momentum {
    val weekly = overload?.weekly.orEmpty()
    if (weekly.isEmpty()) return Momentum(
        "Nothing logged yet",
        "Your first read lands after two sessions on one lift."
    )
    if (weekly.size < 2) return Momentum(
        "Baseline forming",
        "One week on the board. The trend starts with your second."
    )

    val lookback = minOf(MOMENTUM_LOOKBACK_WEEKS, weekly.size - 1)
    val from = weekly[weekly.size - 1 - lookback]
    val current = weekly.last()
    val perMonth = if (from > 0) (current - from) / from / lookback * 4.345 * 100.0 else 0.0
    val lifts = if (liftCount == 1) "1 lift" else "$liftCount lifts"
    val span = if (weekly.size == 1) "1 week" else "${weekly.size} weeks"

    val verdict = when {
        perMonth >= MOMENTUM_DEADBAND_PCT -> "Gaining"
        perMonth <= -MOMENTUM_DEADBAND_PCT -> "Slipping"
        else -> "Holding"
    }
    val rate = when {
        abs(perMonth) < MOMENTUM_DEADBAND_PCT -> "level"
        else -> "%+.1f%% a month".format(perMonth)
    }
    return Momentum(verdict, "$rate across $lifts, $span logged.")
}
