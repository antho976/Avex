package com.forge.app.ui.gym.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.forge.app.program.MuscleGroup
import com.forge.app.ui.gym.stats.components.RowMark
import com.forge.app.ui.gym.stats.components.StatsRow
import com.forge.app.ui.gym.stats.state.BalanceRatioUi
import com.forge.app.ui.gym.stats.state.MuscleSetCount
import com.forge.app.ui.gym.stats.state.RepRangeDist
import kotlin.math.roundToInt

/**
 * A pair needs at least this many combined sets before a ratio means anything. Below it a 2-vs-13
 * week screams "87% skewed" over noise.
 */
internal const val MIN_BALANCE_SETS = 6

/** The pairs Balance draws before either has enough sets to carry a ratio. Vocabulary, not data. */
private val EMPTY_PAIRS = listOf("Push / Pull", "Quad / Ham")

// ── ENOUGH — am I doing the right amount, and of the right things ───────────────────────────────
//
// Three sections, all on the shared row grid. The weekly-tonnage sparkline and the best-vs-typical
// day-type comparison were cut: the hero already owns the page's one trend line, and sets-per-muscle
// against the plan is the actionable version of "am I doing enough".

/** One muscle's week: actual working sets against the plan's target for that muscle. */
private data class MuscleSetRow(val muscle: MuscleGroup, val actual: Int, val target: Int)

private fun muscleRows(
    weekly: List<MuscleSetCount>,
    planned: Map<MuscleGroup, Int>
): List<MuscleSetRow> {
    val actualBy = weekly.associate { it.muscle to it.sets }
    val muscles = (planned.keys + actualBy.keys).toSortedSet(compareBy { it.ordinal })
    return muscles.map { MuscleSetRow(it, actualBy[it] ?: 0, planned[it] ?: 0) }
}

/**
 * Sets per muscle this week. Each muscle's TRACK LENGTH is its own weekly target relative to the
 * biggest one, and the fill is progress toward it: fill the bar and you are on plan, whatever the
 * absolute number.
 */
@Composable
internal fun ColumnScope.SetsPerMuscleContent(
    weekly: List<MuscleSetCount>,
    planned: Map<MuscleGroup, Int>,
    c: StatsColors
) {
    val rows = remember(weekly, planned) { muscleRows(weekly, planned) }
    if (rows.isEmpty()) {
        StatsRow("Sessions logged", "0 of 1", RowMark.Meter(fill = 0f), c.row)
        return
    }
    val ceiling = rows.maxOf { maxOf(it.target, it.actual) }.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth()) {
        rows.forEach { r ->
            val onPlan = r.target > 0 && r.actual >= r.target
            StatsRow(
                label = r.muscle.displayName,
                value = if (r.target > 0) "${r.actual}/${r.target}" else "${r.actual}",
                mark = RowMark.Meter(
                    fill = when {
                        r.target > 0 -> (r.actual.toFloat() / r.target).coerceIn(0f, 1f)
                        r.actual > 0 -> 1f
                        else -> 0f
                    },
                    track = if (r.target > 0) {
                        r.target.toFloat() / ceiling
                    } else {
                        (r.actual.toFloat() / ceiling).coerceAtMost(1f)
                    },
                    dim = !onPlan
                ),
                c = c.row,
                contentDescription = if (r.target > 0) {
                    "${r.muscle.displayName}, ${r.actual} of ${r.target} sets"
                } else {
                    "${r.muscle.displayName}, ${r.actual} sets, no target"
                }
            )
        }
    }
}

/** The sets-per-muscle verdict. */
internal fun setsPerMuscleRead(
    weekly: List<MuscleSetCount>,
    planned: Map<MuscleGroup, Int>
): String {
    val rows = muscleRows(weekly, planned)
    val totalSets = rows.sumOf { it.actual }
    if (rows.isEmpty()) return "Nothing logged"
    val targeted = rows.filter { it.target > 0 }
    if (targeted.isEmpty()) return "$totalSets sets, no plan"
    // An untrained week is not a failed one: before the first set it states the plan, not a verdict.
    if (totalSets == 0) return "0 of ${targeted.sumOf { it.target }} planned"
    val met = targeted.count { it.actual >= it.target }
    return "$met of ${targeted.size} on plan"
}

/** How every logged set splits across strength, hypertrophy and endurance rep ranges. */
@Composable
internal fun ColumnScope.RepRangeContent(dist: RepRangeDist?, c: StatsColors) {
    val d = dist
    val total = (d?.total ?: 0).coerceAtLeast(1)
    val bands = listOf(
        Triple("Heavy, 1-5", d?.strength ?: 0, "heavy"),
        Triple("Mid, 6-12", d?.hypertrophy ?: 0, "mid"),
        Triple("High, 13+", d?.endurance ?: 0, "high")
    )
    Column(Modifier.fillMaxWidth()) {
        bands.forEach { (label, count, spoken) ->
            val pct = if (d == null || d.total == 0) 0 else (count * 100.0 / d.total).roundToInt()
            StatsRow(
                label = label,
                value = "$pct%",
                mark = RowMark.Meter(fill = count.toFloat() / total),
                c = c.row,
                contentDescription = "$spoken reps, $count sets, $pct percent"
            )
        }
    }
}

/** The rep-range verdict. */
internal fun repRangeRead(dist: RepRangeDist?): String {
    val d = dist
    if (d == null || d.total == 0) return "No sets yet"
    return when (maxOf(d.strength, d.hypertrophy, d.endurance)) {
        d.hypertrophy -> "Mostly 6-12"
        d.strength -> "Mostly heavy"
        else -> "Mostly high-rep"
    }
}

/**
 * Structural balance: the boundary between the bright and dim halves IS the ratio, and the tick
 * marks the even split. Pairs with too few sets still draw, empty, rather than vanishing.
 */
@Composable
internal fun ColumnScope.BalanceContent(ratios: List<BalanceRatioUi>, c: StatsColors) {
    val usable = ratios.filter { it.total >= MIN_BALANCE_SETS }
    Column(Modifier.fillMaxWidth()) {
        if (usable.isEmpty()) {
            EMPTY_PAIRS.forEach { title ->
                StatsRow(
                    label = title,
                    value = "0 · 0",
                    mark = RowMark.Split(left = 0f),
                    c = c.row,
                    contentDescription = "$title, no sets logged yet"
                )
            }
            return
        }
        usable.forEach { b ->
            StatsRow(
                label = b.title,
                value = "${b.setsA} · ${b.setsB}",
                mark = RowMark.Split(left = b.setsA.toFloat() / b.total),
                c = c.row,
                contentDescription = "${b.title}, ${b.setsA} ${b.labelA} against " +
                    "${b.setsB} ${b.labelB}. " + if (b.balanced == true) "Balanced." else "Uneven."
            )
        }
    }
}

/** The balance verdict. */
internal fun balanceRead(ratios: List<BalanceRatioUi>): String {
    val usable = ratios.filter { it.total >= MIN_BALANCE_SETS }
    if (usable.isEmpty()) return "Not enough sets"
    val off = usable.firstOrNull { it.balanced == false } ?: return "Even"
    return "leans ${(if (off.setsA >= off.setsB) off.labelA else off.labelB).lowercase()}"
}
