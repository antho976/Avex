package com.forge.app.ui.gym.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// TargetBar + DivergingBar removed 2026-07-01 — sets-per-muscle now sizes each track BY its target
// (fill the bar = hit the plan) and Balance draws a single split-ratio bar. The old shared-scale
// bars with floating target ticks read as confusing stray lines.
//
// MeterRow / ColumnBars / SegmentedBar landed 2026-08-23: the rebuilt Stats page draws five
// different sections through MeterRow alone, so it is extracted here (§2⑥ — second use extracts
// within the feature package) rather than re-rolled per section with drifting bar heights.

/**
 * A horizontal bar split into colored tier zones with a marker sitting on it — the relative-strength
 * "where do I rank" read. [zoneColors] paints the bands left→right; [zoneEdges] are the cumulative
 * 0→1 boundaries between them (size == zoneColors.size - 1); [markerFraction] is the value's position.
 */
@Composable
internal fun BandedBar(
    markerFraction: Float,
    zoneEdges: List<Float>,
    zoneColors: List<Color>,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        val radius = CornerRadius(h / 2, h / 2)
        var startFrac = 0f
        zoneColors.forEachIndexed { i, color ->
            val endFrac = if (i < zoneEdges.size) zoneEdges[i] else 1f
            val x0 = (w * startFrac).coerceIn(0f, w)
            val x1 = (w * endFrac).coerceIn(0f, w)
            if (x1 > x0) drawRoundRect(
                color = color,
                topLeft = Offset(x0, 0f),
                size = Size(x1 - x0, h),
                cornerRadius = radius
            )
            startFrac = endFrac
        }
        val mx = (w * markerFraction.coerceIn(0f, 1f)).coerceIn(0f, w)
        drawLine(
            color = markerColor,
            start = Offset(mx, -3.dp.toPx()),
            end = Offset(mx, h + 3.dp.toPx()),
            strokeWidth = 2.5.dp.toPx()
        )
        drawCircle(color = markerColor, radius = h * 0.6f, center = Offset(mx, h / 2f))
    }
}

/**
 * The page's workhorse row: `label ┃ track ┃ reading`. One value against one target, drawn as a
 * meter (§2② — "one value vs a target → meter bar; at zero, empty track, honest 0").
 *
 * [trackFraction] lets a set of rows encode a SECOND quantity in how far the track itself runs, the
 * way sets-per-muscle sizes each track by that muscle's own weekly target — so a short full bar and
 * a long full bar both read as "on plan" while staying honestly different sizes. Leave it at 1f and
 * every row shares one track.
 *
 * Nothing here has a fixed height: the label and reading grow with the font scale and the row grows
 * with them (§14).
 */
@Composable
internal fun MeterRow(
    label: String,
    reading: String,
    fillFraction: Float,
    fillColor: Color,
    trackColor: Color,
    labelColor: Color,
    readingColor: Color,
    modifier: Modifier = Modifier,
    trackFraction: Float = 1f,
    /** Reads the whole row to TalkBack as one value; the bar itself is decorative once said. */
    contentDescription: String = "$label, $reading"
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            modifier = Modifier.widthIn(min = 76.dp).weight(0.36f)
        )
        Box(Modifier.weight(0.64f).padding(horizontal = 10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(trackFraction.coerceIn(0.08f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor)
            ) {
                if (fillFraction > 0f) Box(
                    Modifier
                        .fillMaxWidth(fillFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(fillColor)
                )
            }
        }
        Text(
            reading,
            style = MaterialTheme.typography.labelMedium,
            color = readingColor,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 48.dp)
        )
    }
}

/** One bar in a [ColumnBars] rail: its height fraction, whether it counts as met, and its own note. */
internal data class ColumnBar(val fraction: Float, val met: Boolean = true, val dimmed: Boolean = false)

/**
 * A rail of vertical bars over a shared baseline, with an optional dashed target rule across it —
 * "value per period" (§2②). At zero every bar is a flat stub on its track, never a hidden section.
 *
 * The rule is a claim about data (the target you are measured against), which is the only thing §1
 * lets a line be.
 */
@Composable
internal fun ColumnBars(
    bars: List<ColumnBar>,
    barColor: Color,
    dimColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    /** 0→1 height of the target rule, or null for no rule. */
    targetFraction: Float? = null,
    ruleColor: Color = dimColor,
    contentDescription: String? = null
) {
    if (bars.isEmpty()) return
    Box(modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription }) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    // The track shows where a bar COULD reach, so an empty week reads as a missed
                    // week rather than as absent data.
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)).background(trackColor))
                    // A true zero draws nothing. The 0.02 floor keeps a small-but-real value
                    // visible; applying it to zero would paint a stripe that claims data.
                    if (bar.fraction > 0f) Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(bar.fraction.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (bar.dimmed || !bar.met) dimColor else barColor)
                    )
                }
            }
        }
        if (targetFraction != null) Canvas(Modifier.fillMaxSize()) {
            val y = size.height * (1f - targetFraction.coerceIn(0f, 1f))
            val dash = 4.dp.toPx()
            drawLine(
                color = ruleColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
            )
        }
    }
}

/** One slice of a [SegmentedBar]: its share of the whole and the color it paints. */
internal data class BarSegment(val fraction: Float, val color: Color)

/**
 * One bar carrying a whole distribution — the split IS the reading, so no percentages are printed
 * beside it. Used for push/pull balance (two segments plus the 50/50 tick) and for the rep-range
 * split (three). [centerTickColor] draws the "even" mark; leave it null where even isn't the ideal.
 */
@Composable
internal fun SegmentedBar(
    segments: List<BarSegment>,
    trackColor: Color,
    modifier: Modifier = Modifier,
    centerTickColor: Color? = null,
    contentDescription: String? = null
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
    ) {
        Row(Modifier.matchParentSize()) {
            segments.filter { it.fraction > 0f }.forEach { seg ->
                Box(Modifier.weight(seg.fraction).fillMaxHeight().background(seg.color))
            }
        }
        if (centerTickColor != null) Box(
            Modifier.align(Alignment.Center).width(2.dp).fillMaxHeight().background(centerTickColor)
        )
    }
}

/**
 * A legend entry for a [SegmentedBar] or a multi-series chart: a swatch and its label, sized from
 * the type scale so it grows with the font setting. The swatch is decorative — the label says it.
 */
@Composable
internal fun BarKey(color: Color, label: String, textColor: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp, 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .clearAndSetSemantics { }
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

/**
 * The n-of-m unlock meter §12 prescribes, for a section whose rows come FROM data and so has no
 * vocabulary to draw before any exists. [need] is the real gate, never a rounded-up guess: a bar
 * filling toward the wrong number unlocks nothing.
 *
 * This is what stands in for the banned alternatives — a status word, a hidden section, or a lone
 * ghost line that reads as broken. It is a reading in its own right: progress toward the first read.
 */
@Composable
internal fun GateMeter(
    label: String,
    have: Int,
    need: Int,
    fillColor: Color,
    trackColor: Color,
    labelColor: Color,
    readingColor: Color,
    modifier: Modifier = Modifier
) {
    MeterRow(
        label = label,
        reading = "$have of $need",
        fillFraction = if (need > 0) have.toFloat() / need else 0f,
        fillColor = fillColor,
        trackColor = trackColor,
        labelColor = labelColor,
        readingColor = readingColor,
        contentDescription = "$label, $have of $need",
        modifier = modifier
    )
}
