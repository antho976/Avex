package com.forge.app.ui.gym.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.forge.app.domain.adapt.E1rm
import com.forge.app.domain.units.WeightUnit
import com.forge.app.domain.units.toDisplayWeight
import com.forge.app.domain.units.unitLabel
import com.forge.app.ui.common.clickableLabeled
import com.forge.app.ui.gym.stats.components.LineChart
import com.forge.app.ui.gym.stats.components.RowMark
import com.forge.app.ui.gym.stats.components.ScatterChart
import com.forge.app.ui.gym.stats.components.StatsRow
import com.forge.app.ui.gym.stats.state.E1rmLift
import com.forge.app.ui.gym.stats.state.PatternAxis
import com.forge.app.ui.gym.stats.state.PlateauFlagUi
import com.forge.app.ui.gym.stats.state.PrEntry
import com.forge.app.ui.gym.stats.state.PrRecency
import com.forge.app.ui.gym.stats.state.RepMaxSet
import com.forge.app.ui.gym.stats.state.StrengthCurve
import com.forge.app.ui.gym.stats.state.TimeToPrEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** A per-lift strength curve only earns its chart once it has this many plotted sets. */
private const val MIN_POINTS_FOR_LIFT_CURVE = 6

/** Bodyweight-relative strength tiers — generic ×bodyweight bands, sex-aware. */
private val TIER_CUTOFFS_MALE = listOf(0.4, 0.7, 1.1, 1.5)
private val TIER_CUTOFFS_FEMALE = listOf(0.3, 0.5, 0.8, 1.1)
private val TIER_FULL = listOf("Untrained", "Novice", "Intermediate", "Advanced", "Elite")

/** A tier only locks once a lift has this many sessions, so one fluke set cannot read as Advanced. */
private const val MIN_SESSIONS_FOR_TIER = 3

/** The rungs the rep-max ladder draws before any weighted set exists. Vocabulary, not data. */
private val EMPTY_REP_RUNGS = listOf(1, 3, 5, 8)

/** The movement patterns the peak comparison draws before any has a logged max. */
private val EMPTY_PATTERNS = listOf("Push", "Pull", "Quads", "Posterior", "Core")

private fun tierCutoffs(sex: String) = if (sex == "female") TIER_CUTOFFS_FEMALE else TIER_CUTOFFS_MALE

internal fun tierIndex(ratio: Double, sex: String): Int {
    tierCutoffs(sex).forEachIndexed { i, cut -> if (ratio < cut) return i }
    return tierCutoffs(sex).size
}

// ── STRONGER — is the weight going up ───────────────────────────────────────────────────────────
//
// Every section is the shared row grid. The depth an experienced lifter wants is behind a lift's
// own row, where it costs the scroll nothing: trend, PR history and drought, usual pace between
// PRs, the load-rep curve, and the ladder's prescription when a lift has stopped moving.

/** One tappable row per lift, ranked against your strongest. */
@Composable
internal fun ColumnScope.LiftList(
    lifts: List<E1rmLift>,
    prs: List<PrEntry>,
    curves: List<StrengthCurve>,
    plateaus: List<PlateauFlagUi>,
    prRecency: PrRecency?,
    timeToPr: List<TimeToPrEntry>,
    weightUnit: WeightUnit,
    c: StatsColors
) {
    if (lifts.isEmpty()) {
        StatsRow("Sessions on a lift", "0 of 2", RowMark.Meter(fill = 0f), c.row)
        return
    }
    val maxE1 = lifts.maxOf { it.currentE1rm }.coerceAtLeast(1.0)
    Column(Modifier.fillMaxWidth()) {
        lifts.forEach { lift ->
            LiftRow(
                lift = lift,
                frac = (lift.currentE1rm / maxE1).toFloat(),
                prsForLift = prs.filter { it.exerciseName == lift.exerciseName },
                curve = curves.firstOrNull { it.exerciseId == lift.exerciseId },
                plateau = plateaus.firstOrNull { it.exerciseId == lift.exerciseId },
                daysSincePr = prRecency?.byExercise?.get(lift.exerciseId),
                usualPrGap = timeToPr.firstOrNull { it.exerciseId == lift.exerciseId },
                weightUnit = weightUnit,
                c = c
            )
        }
    }
}

/** The lift-list verdict. */
internal fun liftListRead(lifts: List<E1rmLift>): String {
    if (lifts.isEmpty()) return "No weighted sets"
    val moving = lifts.count { !it.stalling }
    return "$moving of ${lifts.size} moving"
}

@Composable
private fun LiftRow(
    lift: E1rmLift,
    frac: Float,
    prsForLift: List<PrEntry>,
    curve: StrengthCurve?,
    plateau: PlateauFlagUi?,
    daysSincePr: Int?,
    usualPrGap: TimeToPrEntry?,
    weightUnit: WeightUnit,
    c: StatsColors
) {
    val display = remember(lift.history, weightUnit) { lift.history.map { toDisplayWeight(it, weightUnit) } }
    val expandable = display.size >= 2
    var expanded by rememberSaveable(lift.exerciseId) { mutableStateOf(false) }
    val current = toDisplayWeight(lift.currentE1rm, weightUnit).roundToInt()
    val unit = unitLabel(weightUnit)

    Column(Modifier.fillMaxWidth()) {
        StatsRow(
            label = lift.exerciseName,
            value = "$current $unit",
            mark = RowMark.Meter(fill = frac, dim = lift.stalling),
            c = c.row,
            leading = if (!expandable) null else if (expanded) "▾" else "▸",
            // Every lift row reserves the caret gutter, so a single-session lift still lines up.
            reserveLeading = true,
            modifier = if (expandable) {
                Modifier.clickableLabeled(
                    if (expanded) "Collapse ${lift.exerciseName}" else "Expand ${lift.exerciseName}"
                ) { expanded = !expanded }
            } else Modifier,
            contentDescription = "${lift.exerciseName}, $current $unit estimated max" +
                if (lift.stalling) ", not moving" else ""
        )
        AnimatedVisibility(
            visible = expanded && expandable,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LiftDetail(lift, display, prsForLift, curve, plateau, daysSincePr, usualPrGap, weightUnit, c)
        }
    }
}

/** Everything this app knows about one lift, gathered under its own row. */
@Composable
private fun LiftDetail(
    lift: E1rmLift,
    display: List<Double>,
    prsForLift: List<PrEntry>,
    curve: StrengthCurve?,
    plateau: PlateauFlagUi?,
    daysSincePr: Int?,
    usualPrGap: TimeToPrEntry?,
    weightUnit: WeightUnit,
    c: StatsColors
) {
    val unit = unitLabel(weightUnit)
    val lo = remember(display) { display.minOrNull() ?: 0.0 }
    val hi = remember(display) { display.maxOrNull() ?: 1.0 }
    val fmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp)) {
        LineChart(
            values = display,
            lineColor = c.accent,
            trendColor = c.muted,
            minValue = lo,
            maxValue = hi,
            modifier = Modifier
                .fillMaxWidth()
                .height(STATS_CHART_H)
                .semantics {
                    contentDescription = "${lift.exerciseName} estimated one-rep max over " +
                        "${display.size} sessions, from ${lo.roundToInt()} to ${hi.roundToInt()} $unit."
                }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${lo.roundToInt()} $unit", style = MaterialTheme.typography.labelSmall, color = c.muted)
            lift.monthlyPct?.let { pct ->
                Text("%+.1f%% a month".format(pct), style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
            Text("${hi.roundToInt()} $unit", style = MaterialTheme.typography.labelSmall, color = c.muted)
        }

        prDroughtLine(daysSincePr, usualPrGap)?.let { line ->
            Spacer(Modifier.height(12.dp))
            Text(line, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        val recentPrs = remember(prsForLift) { prsForLift.sortedByDescending { it.date }.take(3) }
        if (recentPrs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            recentPrs.forEach { pr ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${toDisplayWeight(pr.weightLb, weightUnit).roundToInt()} $unit × ${pr.reps}",
                        style = MaterialTheme.typography.bodySmall, color = c.onBg
                    )
                    Text(fmt.format(Date(pr.date)), style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
        }

        if (plateau != null) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(0.5.dp, c.accent, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(plateau.advice, style = MaterialTheme.typography.labelSmall, color = c.onBg)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                plateau.detail,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                fontStyle = FontStyle.Italic
            )
        }

        if (curve != null && curve.points.size >= MIN_POINTS_FOR_LIFT_CURVE) {
            Spacer(Modifier.height(14.dp))
            LiftCurveChart(curve, weightUnit, c)
        }
    }
}

/** "Last PR 12 days ago, against a usual 18." Null when this lift has never set one. */
internal fun prDroughtLine(daysSincePr: Int?, usualPrGap: TimeToPrEntry?): String? {
    if (daysSincePr == null) return null
    val since = if (daysSincePr == 0) "Last PR today" else "Last PR $daysSincePr days ago"
    val usual = usualPrGap?.takeIf { it.prCount >= 2 }?.let { ", against a usual ${it.avgDaysBetween}" }
    return "$since${usual ?: ""}."
}

/** Every working set as weight × reps with the fitted Epley curve — scoped to one lift. */
@Composable
private fun LiftCurveChart(curve: StrengthCurve, weightUnit: WeightUnit, c: StatsColors) {
    val unit = unitLabel(weightUnit)
    val pts = remember(curve, weightUnit) {
        curve.points.map { Offset(it.reps.toFloat(), toDisplayWeight(it.weightLb, weightUnit).toFloat()) }
    }
    val e1 = toDisplayWeight(curve.e1rmLb, weightUnit).toFloat()
    val maxReps = curve.points.maxOf { it.reps }.coerceAtLeast(2)
    val overlay = remember(curve, weightUnit) {
        (maxReps downTo 1).map { r -> Offset(r.toFloat(), E1rm.epleyInverse(e1.toDouble(), r).toFloat()) }
    }
    ScatterChart(
        points = pts,
        overlay = overlay,
        pointColor = c.muted,
        lineColor = c.accent,
        gridColor = c.outline.copy(alpha = 0.15f),
        minX = 1f, maxX = maxReps.toFloat(),
        minY = minOf(pts.minOf { it.y }, overlay.minOf { it.y }) * 0.95f,
        maxY = maxOf(e1, pts.maxOf { it.y }) * 1.05f,
        highlightOverlayEnd = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(STATS_CHART_H)
            .semantics {
                contentDescription = "Every working set on ${curve.exerciseName} as weight " +
                    "against reps, with a projected one-rep max of ${e1.roundToInt()} $unit."
            }
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "Every set, weight × reps. The filled point is your projected single, ${e1.roundToInt()} $unit.",
        style = MaterialTheme.typography.labelSmall, color = c.muted
    )
}

/**
 * Relative strength: each lift's estimated max over your bodyweight, on the tier zones. This is the
 * one place the knowledge ladder is named out loud, and it names it as a measurement of a lift,
 * never as a permission level for the screen.
 */
@Composable
internal fun ColumnScope.StrengthStandardsContent(
    lifts: List<E1rmLift>,
    bodyweightLb: Double?,
    sex: String,
    c: StatsColors
) {
    val bw = bodyweightLb ?: 0.0
    val cutoffs = tierCutoffs(sex)
    val maxRatio = (cutoffs.last() * 1.3).toFloat()
    val edges = cutoffs.map { (it / maxRatio).toFloat() }
    val rated = if (bw > 0.0) {
        lifts.filter { it.currentE1rm > 0 && it.history.size >= MIN_SESSIONS_FOR_TIER }.take(5)
    } else emptyList()

    Column(Modifier.fillMaxWidth()) {
        if (rated.isEmpty()) {
            // The empty ladder still draws: it shows the rungs every lift will be measured on
            // before there is a lift on them, which is the most useful thing this section can do
            // for someone who has never seen the tiers.
            StatsRow(
                label = "Tiers",
                // The span the ladder covers, not its end names: "Untrained to Elite" is the only
                // value on the page long enough to wrap its column. The tier WORD rides the
                // section verdict, where it has the room.
                value = "%.1f-%.1f×".format(cutoffs.first(), cutoffs.last()),
                mark = RowMark.Banded(marker = null, edges = edges),
                c = c.row,
                contentDescription = "The bodyweight-multiple tiers, with no lift on them yet: " +
                    TIER_FULL.joinToString(", ")
            )
            return
        }
        rated.forEach { lift ->
            val ratio = lift.currentE1rm / bw
            StatsRow(
                label = lift.exerciseName,
                value = "%.2f×".format(ratio),
                mark = RowMark.Banded(marker = (ratio / maxRatio).toFloat(), edges = edges),
                c = c.row,
                contentDescription = "${lift.exerciseName}, " +
                    "%.2f times bodyweight, %s".format(ratio, TIER_FULL[tierIndex(ratio, sex)])
            )
        }
    }
}

/** The relative-strength verdict — the tier itself, which is the whole point of the section. */
internal fun strengthStandardsRead(
    lifts: List<E1rmLift>,
    bodyweightLb: Double?,
    sex: String
): String {
    val bw = bodyweightLb ?: 0.0
    if (bw <= 0.0) return "No bodyweight"
    val rated = lifts.filter { it.currentE1rm > 0 && it.history.size >= MIN_SESSIONS_FOR_TIER }
    if (rated.isEmpty()) return "Calibrating"
    return TIER_FULL[tierIndex(rated.maxOf { it.currentE1rm } / bw, sex)]
}

/**
 * The rep-max ladder: your best weight at each rep count on the lift you train most. The most
 * directly usable number on the page at any level, because it answers "what goes on the bar for a
 * set of eight" from your own history rather than from a formula.
 */
@Composable
internal fun ColumnScope.RepMaxContent(repMaxes: RepMaxSet?, weightUnit: WeightUnit, c: StatsColors) {
    val entries = repMaxes?.entries.orEmpty()
    val unit = unitLabel(weightUnit)
    Column(Modifier.fillMaxWidth()) {
        if (entries.isEmpty()) {
            EMPTY_REP_RUNGS.forEach { reps ->
                StatsRow(
                    label = if (reps == 1) "1 rep" else "$reps reps",
                    value = "0 $unit",
                    mark = RowMark.Meter(fill = 0f),
                    c = c.row,
                    contentDescription = "No best at $reps reps yet"
                )
            }
            return
        }
        val ceiling = entries.maxOf { it.weightLb }.coerceAtLeast(1.0)
        entries.forEach { e ->
            val shown = toDisplayWeight(e.weightLb, weightUnit).roundToInt()
            StatsRow(
                label = if (e.reps == 1) "1 rep" else "${e.reps} reps",
                value = "$shown $unit",
                mark = RowMark.Meter(fill = (e.weightLb / ceiling).toFloat()),
                c = c.row,
                contentDescription = "Best at ${e.reps} reps, $shown $unit"
            )
        }
    }
}

/** The rep-max verdict — which lift the ladder is for. */
internal fun repMaxRead(repMaxes: RepMaxSet?): String = repMaxes?.exerciseName ?: "No rep maxes"

/**
 * Each movement pattern against its OWN all-time peak. Absolute load is not comparable across
 * patterns, so this is the only honest cross-pattern read: a pattern at 78% has fallen off,
 * whatever the number on the bar.
 */
@Composable
internal fun ColumnScope.PatternContent(axes: List<PatternAxis>, c: StatsColors) {
    Column(Modifier.fillMaxWidth()) {
        if (axes.isEmpty()) {
            EMPTY_PATTERNS.forEach { name ->
                StatsRow(
                    label = name,
                    value = "0%",
                    mark = RowMark.Meter(fill = 0f),
                    c = c.row,
                    contentDescription = "$name, no logged max yet"
                )
            }
            return
        }
        axes.forEach { axis ->
            val pct = (axis.fraction * 100).roundToInt()
            StatsRow(
                label = axis.label.lowercase().replaceFirstChar { it.uppercase() },
                value = "$pct%",
                mark = RowMark.Meter(fill = axis.fraction.toFloat(), dim = axis.fraction < 0.95),
                c = c.row,
                contentDescription = "${axis.label}, $pct percent of your peak"
            )
        }
    }
}

/** The movement-pattern verdict. */
internal fun patternRead(axes: List<PatternAxis>): String {
    if (axes.isEmpty()) return "No patterns yet"
    val atPeak = axes.count { it.fraction >= 0.95 }
    if (atPeak == axes.size) return "All at peak"
    return "$atPeak of ${axes.size} at peak"
}
