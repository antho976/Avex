package com.forge.app.ui.gym.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * **The page's only row.** Three columns, and every section on Stats uses them, so the whole scroll
 * has one left edge, one bar column and one right edge:
 *
 * ```
 * Chest      ▓▓▓▓▓▓▓▓▓░░░     14/14
 * Push/Pull  ▓▓▓▓▓▓│▓▓▓▓▓     23·25
 * Deadlift   ▓▓▓▓▓▓▓▓▓█░    2.28× Elite
 * └ label ──┘└─ mark ────┘└─ value ─┘
 * ```
 *
 * The three [RowMark] treatments differ only in how the bar is FILLED, never in geometry, so a
 * meter, a split and a banded ladder still read as the same instrument seen three ways. That is the
 * whole reason this exists: the page had five mark layouts alternating between indented and
 * full-bleed, and it read as noise rather than as a system.
 *
 * Nothing here has a fixed height. The label and value grow with the font scale and the row grows
 * with them (§14).
 */
@Composable
internal fun StatsRow(
    label: String,
    value: String,
    mark: RowMark,
    c: RowColors,
    modifier: Modifier = Modifier,
    /**
     * An expand caret for a row that drills in. Rows that do not drill in still RESERVE the gutter
     * (§8), so every label on the page keeps one left edge whether or not its row opens. It lives
     * here rather than appended to [value] because a trailing glyph wraps the value to a second
     * line at 200% font scale.
     */
    leading: String? = null,
    reserveLeading: Boolean = leading != null,
    contentDescription: String = "$label, $value"
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (reserveLeading) {
            Text(
                leading.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = c.fill,
                modifier = Modifier.width(LEADING_GUTTER)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = c.label,
            modifier = Modifier.weight(LABEL_COLUMN)
        )
        Box(Modifier.weight(MARK_COLUMN).padding(horizontal = 10.dp)) { RowBar(mark, c) }
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = c.value,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(VALUE_COLUMN)
        )
    }
}

/** The one grid every row on the page is measured against. */
// Weighted so the value column still holds "405 lb ▸" on one line at 200% font scale, which is
// where the three columns are under the most pressure (§14).
private const val LABEL_COLUMN = 0.32f
private const val MARK_COLUMN = 0.40f
private const val VALUE_COLUMN = 0.28f

/** The caret gutter, reserved on every row of a section where any row drills in. */
private val LEADING_GUTTER = 16.dp

/** Bar height, shared so a meter, a split and a banded row are the same instrument. */
private val BAR_HEIGHT = 10.dp

/** Colors a row draws from, so callers pass one object rather than six parameters. */
internal data class RowColors(
    val label: Color,
    val value: Color,
    val fill: Color,
    val dim: Color,
    val track: Color,
    val tick: Color
)

/** How a row's bar is filled. Geometry is identical across all three. */
internal sealed interface RowMark {
    /**
     * One value against one target. [track] shortens the bar itself so a set of rows can encode a
     * SECOND quantity in how far each track runs — the way each muscle's track is its own weekly
     * target, making a short full bar and a long full bar both read as "on plan".
     */
    data class Meter(val fill: Float, val track: Float = 1f, val dim: Boolean = false) : RowMark

    /** A whole split between two sides, with a tick on the even point. The boundary IS the ratio. */
    data class Split(val left: Float) : RowMark

    /**
     * Zones with a marker sitting on them. [zoneColors] is explicit because the two ladders on the
     * page mean opposite things: on the strength tiers a brighter zone is further along and the
     * accent ramp says so, while on the fatigue scale the top zone is the one to avoid and takes
     * §5's reserved true-state colors. Same geometry, different claim.
     */
    data class Banded(
        val marker: Float?,
        val edges: List<Float>,
        val zoneColors: List<Color>? = null
    ) : RowMark
}

@Composable
private fun RowBar(mark: RowMark, c: RowColors) {
    when (mark) {
        is RowMark.Meter -> Box(
            Modifier
                .fillMaxWidth(mark.track.coerceIn(0.08f, 1f))
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(50))
                .background(c.track)
        ) {
            // A true zero draws nothing: a stub would claim data that is not there.
            if (mark.fill > 0f) Box(
                Modifier
                    .fillMaxWidth(mark.fill.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (mark.dim) c.dim else c.fill)
            )
        }

        is RowMark.Split -> Box(
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(50))
                .background(c.track)
        ) {
            if (mark.left > 0f) Row(Modifier.matchParentSize()) {
                Box(Modifier.weight(mark.left).fillMaxHeight().background(c.fill))
                if (mark.left < 1f) Box(Modifier.weight(1f - mark.left).fillMaxHeight().background(c.dim))
            }
            // The even point. Drawn in the page's own ground so it reads as a notch cut through the
            // bar wherever the boundary lands, including monochrome where the fill IS the foreground.
            Box(Modifier.align(Alignment.Center).width(2.dp).fillMaxHeight().background(c.tick))
        }

        is RowMark.Banded -> Box(
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                // Clip the WHOLE bar once. Rounding each zone instead turned the ladder into a row
                // of separate pills with gaps between them, which read as five bars, not one scale.
                .clip(RoundedCornerShape(50))
                .background(c.track)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val zones = mark.edges.size + 1
                var startFrac = 0f
                (0 until zones).forEach { i ->
                    val endFrac = if (i < mark.edges.size) mark.edges[i] else 1f
                    val x0 = (size.width * startFrac).coerceIn(0f, size.width)
                    val x1 = (size.width * endFrac).coerceIn(0f, size.width)
                    if (x1 > x0) drawRect(
                        color = mark.zoneColors?.getOrNull(i)
                            ?: androidx.compose.ui.graphics.lerp(c.track, c.fill, i / (zones - 1f)),
                        topLeft = Offset(x0, 0f),
                        size = Size(x1 - x0, size.height)
                    )
                    startFrac = endFrac
                }
            }
            // The same notch a split row uses, so a marker and a boundary read as one vocabulary.
            mark.marker?.let { m ->
                Box(
                    Modifier
                        .fillMaxWidth(m.coerceIn(0f, 1f))
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(c.tick))
                }
            }
        }
    }
}
