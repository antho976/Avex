package com.forge.app.ui.gym.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.forge.app.ui.common.statsEntrance
import com.forge.app.ui.gym.stats.components.RowColors
import com.forge.app.ui.theme.MonoSectionAnchor

/** Theme colors bundled so the per-section builders don't each take a fistful of color params. */
internal data class StatsColors(
    val onBg: Color,
    val muted: Color,
    val accent: Color,
    val outline: Color,
    val background: Color
) {
    /** The palette every [com.forge.app.ui.gym.stats.components.StatsRow] on the page draws from. */
    val row: RowColors
        get() = RowColors(
            label = onBg,
            value = muted,
            fill = accent,
            dim = accent.copy(alpha = 0.25f),
            track = outline.copy(alpha = 0.15f),
            tick = background
        )
}

// ── One spacing/size scale so every section lines up ────────────────────────────────────────────
/** The screen's horizontal gutter — §7's page gutter. */
internal val STATS_GUTTER = 24.dp
/** The standard chart height, used only inside a lift's drill-down. */
internal val STATS_CHART_H = 132.dp
/** The hero's sparkline — the one chart on the page's own scroll. */
internal val STATS_RAIL_H = 64.dp

/**
 * **The page's one section shape.** A mono anchor with its verdict on the SAME line, then rows.
 *
 * ```
 * SETS PER MUSCLE              3 of 5 on plan
 * Chest      ▓▓▓▓▓▓▓▓▓░░░              14/14
 * Back       ▓▓▓▓▓▓▓▓▓▓▓▓              16/16
 * ```
 *
 * The verdict rides the header rather than opening a paragraph beneath it. An earlier build gave
 * every section a two-line sentence carrying the verdict and its reading; stacked sixteen times
 * that read as a wall of grey prose rather than as a system, and the numbers were already in the
 * rows. Keep [verdict] to a few words — it must not wrap at 100% scale.
 *
 * That single line is still what lets one page serve every level: someone in their second week
 * reads the right-hand side of the header, someone in their sixth year reads the rows. Nothing is
 * hidden from either.
 *
 * Open editorial (§1): no card, no fill, no hairline. Air plus the anchor separate sections.
 */
@Composable
internal fun StatsRead(
    c: StatsColors,
    anchor: String,
    verdict: String,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().statsEntrance(index)) {
        Spacer(Modifier.height(18.dp))
        Column(Modifier.padding(horizontal = STATS_GUTTER)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    anchor.uppercase(),
                    style = MonoSectionAnchor,
                    color = c.muted,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                Text(verdict, style = MaterialTheme.typography.bodySmall, color = c.onBg)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
