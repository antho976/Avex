package com.forge.app.ui.gym.train.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.forge.app.domain.units.formatVolume
import com.forge.app.ui.common.EditorialHeader
import com.forge.app.ui.common.statsEntrance
import com.forge.app.ui.theme.ForgePrGold
import com.forge.app.ui.theme.LocalForgeSettings
import com.forge.app.ui.gym.train.state.ExerciseHighlight

/**
 * ONE vertical padding for every row on this sheet (§7) — the recap lifts and the coach's coverage
 * readings share it, so the sheet reads as a single rhythm rather than two stacked lists.
 */
private val SUMMARY_ROW_PAD = 6.dp

/**
 * The coach's corner of the summary: first his read of THIS session, then how much effort signal
 * the session actually carried (per-set RPE + per-exercise "how hard it felt") with an ask to log
 * more so he can calibrate load and rest. Purely informational; effort capture itself stays inline
 * during the session.
 *
 * Two fixes to how this section READ (2026-08-24). The coverage used to sit in a bordered grey box:
 * §1 gives a fill only to something you can tap, so the readings are drawn on the sheet and carry
 * it themselves. And the hierarchy was inverted — the coach's own read was the dimmest, smallest
 * text in its section while the capture nudge below it was brighter and larger. His read now takes
 * the substantive `bodyMedium`/onBg voice the Coach tab already gives it, and everything under it
 * steps down to muted, so the section reads top to bottom instead of competing with itself.
 */
@Composable
internal fun CoachReadSection(
    coachOpinion: String?,
    setsWithRpe: Int,
    totalSets: Int,
    exercisesRated: Int,
    exercisesLogged: Int,
    onBg: Color,
    muted: Color,
    accent: Color
) {
    // Nothing logged → nothing for the coach to read.
    if (coachOpinion == null && exercisesLogged == 0) return
    Column(Modifier.fillMaxWidth().padding(top = 28.dp).statsEntrance(4)) {
        EditorialHeader(label = "What the coach sees", muted = muted, accent = accent)
        Spacer(Modifier.height(10.dp))
        // The coach's substantive line, in the same voice the Coach tab gives it (CoachBlock):
        // bodyMedium on onBg. Italic muted is his SUB-line voice, and using it here made the read
        // itself the faintest thing in its own section.
        coachOpinion?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = onBg)
            Spacer(Modifier.height(18.dp))
        }
        CoachCaptureNudge(setsWithRpe, totalSets, exercisesRated, exercisesLogged, onBg, muted)
    }
}

/**
 * The "give me more to work with" block: a quiet confirmation when every lift and set carried its
 * effort signal, otherwise the coverage as two readings above a short ask. §4.9 — the reading
 * below a gate is progress toward it ("3 of 8 lifts"), so it stays legible before anything unlocks.
 */
@Composable
private fun CoachCaptureNudge(
    setsWithRpe: Int,
    totalSets: Int,
    exercisesRated: Int,
    exercisesLogged: Int,
    onBg: Color,
    muted: Color
) {
    if (exercisesLogged == 0) return
    val effortComplete = exercisesRated >= exercisesLogged
    val rpeComplete = totalSets == 0 || setsWithRpe >= totalSets
    if (effortComplete && rpeComplete) {
        Text(
            "Full effort data this session. He can read exactly how hard it landed and tune the " +
                "next one precisely.",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        return
    }
    CoverageRow("Effort", exercisesRated, exercisesLogged, "lifts", onBg, muted)
    if (totalSets > 0) CoverageRow("RPE", setsWithRpe, totalSets, "sets", onBg, muted)
    Spacer(Modifier.height(10.dp))
    Text(
        "Rate how hard each set feels next time and the coach can dial in your load and rest.",
        style = MaterialTheme.typography.bodySmall,
        color = muted
    )
}

/**
 * One coverage reading: mono label, mono count on the end. Both voices are mono so the pair reads
 * as METRIC meta under the coach's line (§6 — row/metric labels are labelLarge) rather than as a
 * second sentence competing with it.
 */
@Composable
private fun CoverageRow(label: String, done: Int, total: Int, unit: String, onBg: Color, muted: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = SUMMARY_ROW_PAD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            modifier = Modifier.weight(1f)
        )
        // Lower case, matching the "4 sets · 3.2k lb" meta on the recap rows just above (§11 —
        // small-caps is for the LABEL, not for mono meta).
        Text("$done of $total $unit", style = MaterialTheme.typography.labelLarge, color = onBg)
    }
}

/** One lift from the session: what you did on it, and the gold star if it was a PR. */
@Composable
internal fun HighlightRow(h: ExerciseHighlight, onBg: Color, muted: Color) {
    val weightUnit = LocalForgeSettings.current.weightUnit
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = SUMMARY_ROW_PAD),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(h.exerciseName, style = MaterialTheme.typography.bodyMedium, color = onBg)
            Text(
                "${h.setsLogged} sets · ${formatVolume(h.volumeLb, weightUnit)}",
                style = MaterialTheme.typography.labelSmall,
                color = muted
            )
        }
        // §5 — PR gold is reserved for exactly this mark, and the live set row already draws it,
        // so a PR reads the same here as it did the moment it landed.
        if (h.isPr) {
            Text(
                "★",
                style = MaterialTheme.typography.labelSmall,
                color = ForgePrGold,
                modifier = Modifier.semantics { contentDescription = "Personal record" }
            )
        }
    }
}
