package com.forge.app.ui.gym.train.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.forge.app.domain.mood.Mood
import com.forge.app.domain.notify.PrMilestone
import com.forge.app.domain.units.formatVolumeCompact
import com.forge.app.domain.units.unitLabel
import com.forge.app.service.ForgeNotifications
import com.forge.app.ui.common.ConfettiOverlay
import com.forge.app.ui.common.EditorialCountUpFigure
import com.forge.app.ui.common.EditorialFigure
import com.forge.app.ui.common.EditorialHeader
import com.forge.app.ui.common.ForgeHeroAction
import com.forge.app.ui.gym.stats.components.BodyHeatmap
import com.forge.app.ui.common.statsEntrance
import com.forge.app.ui.theme.LocalForgeSettings
import com.forge.app.ui.gym.train.state.SessionSummary

/**
 * The end-of-session summary, drawn as the **Modal** archetype (DESIGN §3): the sheet keeps its
 * `surface` fill, its content sits on the 24dp gutter, and its one action lands at the END.
 *
 * Four reads, in order: any new PRs, the session's own figures, a recap of what you worked (muscle
 * map + exercise list), and the coach's corner — what he saw and the effort data he'd like more of.
 * Sections separate by air and a mono anchor, never a hairline (§1). [onDismiss] keeps its
 * tags/mood params for the shared event signature, but both are now always empty; the mid-session
 * journal flows straight through so it's still persisted on complete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummarySheet(
    summary: SessionSummary,
    onDismiss: (mood: Mood?, tags: List<String>, journal: String) -> Unit
) {
    // Block swipe-to-dismiss: the only way out is the Complete button, otherwise the sheet can be
    // swiped away while the summary is still "open", leaving FINISH dead.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    val context = LocalContext.current
    val hapticStrength = LocalForgeSettings.current.hapticStrength

    // A PR is the peak of a session — celebrate it once with confetti + a single haptic. Both are
    // rememberSaveable so a rotation while the sheet is open doesn't replay them.
    val hasPr = summary.prCount > 0
    var showConfetti by rememberSaveable { mutableStateOf(hasPr) }
    val haptic = LocalHapticFeedback.current
    var prHapticFired by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!prHapticFired && hasPr && hapticStrength != "off") {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            prHapticFired = true
        }
    }
    // PR-milestone push (#13): a finished session crossing a lifetime-PR round number drops a
    // celebratory notification in the shade. Own channel; fired once, only on a genuine milestone.
    var prMilestoneFired by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!prMilestoneFired) {
            PrMilestone.check(summary.lifetimePrCount, summary.prCount)?.let { nudge ->
                ForgeNotifications.postPrMilestone(context, nudge.title, nudge.body)
            }
            prMilestoneFired = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        // §5 — a modal is the one place a fill is not earned by interactivity, and it says `surface`.
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Box {
            SessionSummaryContent(summary) { onDismiss(null, emptyList(), summary.initialJournal) }

            // PR confetti: fires once when the sheet opens on a session with a new PR.
            if (showConfetti) {
                ConfettiOverlay(
                    modifier = Modifier.matchParentSize(),
                    onComplete = { showConfetti = false }
                )
            }
        }
    }
}

/**
 * The sheet's body, split out the way `ModalRecipe` splits its own: the sheet chrome adds nothing
 * you need to design, and on its own the content previews and screenshots without a dialog window.
 */
@Composable
internal fun SessionSummaryContent(summary: SessionSummary, onComplete: () -> Unit) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val weightUnit = LocalForgeSettings.current.weightUnit
    val hasPr = summary.prCount > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        // Header — mono eyebrow over the serif day name, the §3 modal opening. No "workout
        // complete" line under it: the confetti, the figures and the Complete button
        // already say so, and §4.3 cuts mechanics narration rather than trimming it.
        Text(
            summary.dayWord.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = muted
        )
        Spacer(Modifier.height(8.dp))
        // §11 — a serif title takes no terminal period.
        Text(
            summary.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = onBg
        )

        // New PRs — the peak of the session, alone on its line so the isolation carries the
        // emphasis without spending a second serif size on it. Hidden at zero: unlike the
        // readings below, a PR count of 0 is not an honest zero, it is a result that
        // didn't happen, and the gold star on each PR row is where the detail lives.
        if (hasPr) {
            Spacer(Modifier.height(24.dp))
            EditorialCountUpFigure(
                value = summary.prCount.toDouble(),
                label = if (summary.prCount == 1) "new PR" else "new PRs",
                onBg = onBg,
                muted = muted,
                modifier = Modifier.statsEntrance(0)
            )
        }

        // The four readings the session is judged on. Honest zeros, and they wrap rather
        // than clip at 200% font scale (§14) because each figure owns half the row.
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().statsEntrance(1),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EditorialCountUpFigure(
                value = summary.totalVolumeLb,
                // The unit rides in the mono caption, not in the serif figure — a figure
                // clamps to one line, and "12.3k kg" is what clips first at large scales.
                label = "volume · ${unitLabel(weightUnit)}",
                onBg = onBg,
                muted = muted,
                modifier = Modifier.weight(1f)
            ) { formatVolumeCompact(it, weightUnit, withUnit = false) }
            EditorialFigure(
                value = "${summary.durationMinutes}",
                label = "minutes",
                onBg = onBg, muted = muted, accent = accent,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().statsEntrance(2),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EditorialFigure(
                value = "${summary.setCount}",
                label = "sets",
                onBg = onBg, muted = muted, accent = accent,
                modifier = Modifier.weight(1f)
            )
            EditorialFigure(
                value = "${summary.exercisesLogged}",
                // Skipped lifts qualify the figure from its caption rather than crowding
                // the number — the figure states what you actually logged.
                label = if (summary.exercisesSkipped > 0) {
                    "exercises · ${summary.exercisesSkipped} skipped"
                } else {
                    "exercises"
                },
                onBg = onBg, muted = muted, accent = accent,
                modifier = Modifier.weight(1f)
            )
        }

        // Recap — what you worked on: the muscle map + the exercise list.
        if (summary.setsByMuscle.isNotEmpty() || summary.highlights.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            EditorialHeader(label = "What you worked", muted = muted, accent = accent)
            Spacer(Modifier.height(10.dp))
            if (summary.setsByMuscle.isNotEmpty()) {
                BodyHeatmap(
                    setsByMuscle = summary.setsByMuscle,
                    accent = accent,
                    faint = outline.copy(alpha = 0.34f),
                    silhouette = outline.copy(alpha = 0.26f),
                    labelColor = muted,
                    modifier = Modifier.fillMaxWidth().statsEntrance(3)
                )
                Spacer(Modifier.height(10.dp))
            }
            summary.highlights.forEach { h -> HighlightRow(h, onBg = onBg, muted = muted) }
        }

        // Coach — what he read from this session, and the effort data he'd like more of.
        CoachReadSection(
            coachOpinion = summary.coachOpinion,
            setsWithRpe = summary.setsWithRpe,
            totalSets = summary.setCount,
            exercisesRated = summary.exercisesRated,
            exercisesLogged = summary.exercisesLogged,
            onBg = onBg,
            muted = muted,
            accent = accent
        )

        // The single exit from a finishing moment, so it takes the accent-filled hero rather than
        // the light ① capsule (§8, 2026-08-24) — `onPrimary` flips with the accent's luminance, so
        // the label holds on a pale Gold as well as on a monochrome neutral.
        Spacer(Modifier.height(28.dp))
        ForgeHeroAction(
            text = "Complete",
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
