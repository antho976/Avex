package com.forge.app.ui.gym.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forge.app.domain.units.WeightUnit
import com.forge.app.ui.common.SegmentPill
import com.forge.app.ui.common.statsEntrance
import com.forge.app.ui.gym.stats.state.StatsUiState
import com.forge.app.ui.theme.LocalForgeSettings

/*
 * THESIS: Stats answers the four questions a lifter actually asks, in their words, and every answer
 * carries both its verdict and the number it came from. It refuses the category default of a stats
 * page cut by data type (charts of volume, charts of PRs) and the second default of a beginner mode
 * that hides the instruments — nothing here unlocks.
 *
 * OWN-WORLD: the app's open editorial (DESIGN.md §1) — content directly on warm near-black, three
 * type voices, one user-chosen accent at fixed intensities, no card around anything passive. The
 * page's own contribution is one repeated grammar: a mono anchor carrying its verdict on the same
 * line, then rows on a single three-column grid — label, mark, value. Every section, no exceptions;
 * the marks differ only in how the bar is filled, never in where it sits. Eleven sections, one row grid: one left edge, one bar column, one right edge.
 *
 * STORY: the lifter opens Stats, reads whether the needle moved, picks the question they came with,
 * and gets an answer they can act on at whatever depth they read at. Nothing asks them to level up
 * into it.
 *
 * FIRST VIEWPORT: mono eyebrow (STATS · WEEK OF <date>) → one serif verdict on momentum → the
 * reading behind it → the twelve-week estimated-max sparkline → this week's sessions, sets and PRs
 * as three open figures → the four lens pills. No primary action: Stats is a reading surface, and
 * the one thing it could ask for (log a session) belongs to Home.
 *
 * FORM: lens pills cut by question rather than by data type, chosen by Antho over a single long
 * scroll and over keeping the old Strength / Volume / Effort / Days cut.
 */

/**
 * The four questions the page is cut by. Labels are the question in the shortest words that still
 * ask it, since a lens pill that says "Volume" names a data type rather than a thing you wanted
 * to know (§4.4).
 */
enum class StatsLens(val label: String) {
    SHOW_UP("Show up"),
    STRONGER("Stronger"),
    ENOUGH("Enough"),
    RECOVER("Recover")
}

/**
 * Gym → Stats, rebuilt 2026-08-23.
 *
 * Every section renders on every open at its honest zero. There is no beginner mode, no unlock
 * meter over an instrument, and no "not enough data yet": below a gate a section says what still
 * has to happen for its reading to exist (§4.9), which is itself a reading. The depth a lifter
 * needs is carried by wording, not by whether a section appears — the verdict half of each line
 * serves someone in their second week and the reading half serves someone in their sixth year.
 */
@Composable
fun StatsContent(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reopen on the last lens the user viewed. Restore once, THEN start persisting changes.
    val lastLensName by viewModel.lastStatsTabName.collectAsStateWithLifecycle()
    var lens by rememberSaveable { mutableStateOf(StatsLens.STRONGER) }
    var restoredLens by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(lastLensName) {
        if (!restoredLens && lastLensName != null) {
            StatsLens.entries.firstOrNull { it.name == lastLensName }?.let { lens = it }
            restoredLens = true
        }
    }
    LaunchedEffect(restoredLens) {
        if (restoredLens) snapshotFlow { lens }.collect { viewModel.saveStatsTab(it.name) }
    }

    StatsPage(state, lens, onLens = { lens = it }, modifier = modifier)
}

/**
 * The page itself, with no ViewModel and no Hilt — so every lens can be rendered against a known
 * state in a screenshot test, which is the only thing that can check what a regex cannot: that a
 * section survives 200% font scale, that two marks do not collide, that a zero state reads as data
 * rather than as damage.
 */
@Composable
internal fun StatsPage(
    state: StatsUiState,
    lens: StatsLens,
    onLens: (StatsLens) -> Unit,
    modifier: Modifier = Modifier
) {
    val weightUnit = LocalForgeSettings.current.weightUnit
    val c = StatsColors(
        onBg = MaterialTheme.colorScheme.onBackground,
        muted = MaterialTheme.colorScheme.onSurfaceVariant,
        accent = MaterialTheme.colorScheme.primary,
        outline = MaterialTheme.colorScheme.outline,
        background = MaterialTheme.colorScheme.background
    )

    Column(modifier.fillMaxSize()) {
        if (state.loadError) {
            Text(
                "Your stats did not load. Swipe away and back, and restart the app if it keeps happening.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = STATS_GUTTER, vertical = 12.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // With no top bar above it (§4.6), the page owns its own breathing room under the
            // status bar; §7 wants more air above a heading than below it.
            contentPadding = PaddingValues(top = 16.dp, bottom = 56.dp)
        ) {
            item("hero") {
                Column(
                    Modifier.fillMaxWidth().statsEntrance(0)
                        .padding(horizontal = STATS_GUTTER, vertical = 10.dp)
                ) {
                    StatsHeroContent(state, c)
                }
            }

            item("lens") {
                Column(
                    Modifier.fillMaxWidth().statsEntrance(1)
                        .padding(horizontal = STATS_GUTTER, vertical = 12.dp)
                ) {
                    // Four pills stop fitting one line somewhere past 150% font scale, and a lens
                    // the user cannot reach is lost content (§14). Scrolling keeps every one
                    // reachable without wrapping the row into two ragged lines.
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsLens.entries.forEach { entry ->
                            SegmentPill(
                                text = entry.label,
                                selected = entry == lens,
                                onClick = { onLens(entry) },
                                accent = c.accent, onBg = c.onBg, muted = c.muted, outline = c.outline
                            )
                        }
                    }
                }
            }

            when (lens) {
                StatsLens.SHOW_UP -> showUpLens(state, c)
                StatsLens.STRONGER -> strongerLens(state, weightUnit, c)
                StatsLens.ENOUGH -> enoughLens(state, c)
                StatsLens.RECOVER -> recoverLens(state, c)
            }

            item("bottom-gap") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── SHOW UP ─────────────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.showUpLens(state: StatsUiState, c: StatsColors) {
    section("cadence", "Cadence", cadenceRead(state.consistencyStreak, state.weeklySessionCounts), 2, c) {
        CadenceContent(state.weeklySessionCounts, c)
    }
    section("frequency", "What you train", exerciseFrequencyRead(state.exerciseFrequency), 3, c) {
        ExerciseFrequencyContent(state.exerciseFrequency, c)
    }
}

// ── STRONGER ────────────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.strongerLens(state: StatsUiState, weightUnit: WeightUnit, c: StatsColors) {
    section("lifts", "Your lifts", liftListRead(state.e1rmLifts), 2, c) {
        LiftList(
            lifts = state.e1rmLifts,
            prs = state.recentPrs,
            curves = state.strengthCurves,
            plateaus = state.plateauFlags,
            prRecency = state.prRecency,
            timeToPr = state.timeToPr,
            weightUnit = weightUnit,
            c = c
        )
    }
    val bodyweight = state.bodyweightPoints.lastOrNull()?.weightLb
    section(
        "standards", "Where you stand",
        strengthStandardsRead(state.e1rmLifts, bodyweight, state.userSex), 3, c
    ) {
        StrengthStandardsContent(state.e1rmLifts, bodyweight, state.userSex, c)
    }
    section("repmax", "Rep maxes", repMaxRead(state.repMaxes), 4, c) {
        RepMaxContent(state.repMaxes, weightUnit, c)
    }
    section("patterns", "Against your peak", patternRead(state.patternAxes), 5, c) {
        PatternContent(state.patternAxes, c)
    }
}

// ── ENOUGH ──────────────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.enoughLens(state: StatsUiState, c: StatsColors) {
    section(
        "muscles", "Sets per muscle",
        setsPerMuscleRead(state.weeklySetsByMuscle, state.plannedSetsByMuscle), 2, c
    ) {
        SetsPerMuscleContent(state.weeklySetsByMuscle, state.plannedSetsByMuscle, c)
    }
    section("reprange", "Rep ranges", repRangeRead(state.repRange), 3, c) {
        RepRangeContent(state.repRange, c)
    }
    section("balance", "Balance", balanceRead(state.balanceRatios), 4, c) {
        BalanceContent(state.balanceRatios, c)
    }
}

// ── RECOVER ─────────────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.recoverLens(state: StatsUiState, c: StatsColors) {
    section("fatigue", "Fatigue", fatigueRead(state.readinessPulse), 2, c) {
        FatigueContent(state.readinessPulse, state.readinessThreshold, c)
    }
    section("effort", "Effort", effortRead(state.avgRpe, state.avgRpePerSession), 3, c) {
        EffortContent(state.rpeDistribution, state.avgRpe, state.avgRpePerSession, c)
    }
    section("effortmix", "How it felt", effortMixRead(state.weeklyEffort), 4, c) {
        EffortMixContent(state.weeklyEffort, c)
    }
}

/**
 * One section of the page. [verdict] comes from that section's pure `…Read` function, so the words
 * and the rows are derived from the same state and cannot disagree, and the wording stays testable
 * without Compose.
 */
private fun LazyListScope.section(
    key: String,
    anchor: String,
    verdict: String,
    index: Int,
    c: StatsColors,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    item(key) {
        StatsRead(c, anchor = anchor, verdict = verdict, index = index) { content() }
    }
}
