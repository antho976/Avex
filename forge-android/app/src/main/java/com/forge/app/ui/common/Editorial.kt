package com.forge.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.MonoSectionAnchor

/**
 * The app-wide "open editorial" building blocks — the language of the Home screen, the live-session
 * screen and the reworked Profile: content sits DIRECTLY on the near-black page (no boxed grey
 * cards), big serif figures with tiny monospace small-caps labels, hairlines and whitespace for
 * structure. Bordered pills stay reserved for interactive controls; modals keep their surfaces.
 */

/**
 * A quiet small-caps section anchor (mono label + optional right-aligned accent action). Marked as a
 * TalkBack heading. Structure comes from spacing and hairlines, not from a box around the section.
 */
@Composable
fun EditorialHeader(
    label: String,
    muted: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().then(if (onAction != null) Modifier.bounceClick { onAction() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            // The mono SECTION-ANCHOR rung (15sp) — one step above the 13sp its rows sit on, so the
            // anchor leads on size rather than relying on tracking. At a matched 13sp a short header
            // like "BODY" carried less visual mass than the "BODY FAT" row beneath it and read as
            // the smaller of the two (§6, 2026-07-25; was labelLarge 13sp from 2026-07-05).
            style = MonoSectionAnchor,
            color = muted,
            modifier = Modifier.semantics { heading() }
        )
        if (action != null) Text(action, style = MaterialTheme.typography.labelSmall, color = accent)
    }
}

/** The full-width hairline used between open sections (the Home-screen rhythm). */
@Composable
fun EditorialHairline(outline: Color, modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, color = outline.copy(alpha = 0.25f))
}

/**
 * One open dashboard figure: a large serif number set straight on the background with its small-caps
 * mono label underneath — no card, no border. The optional signed [delta] rides beside the figure as
 * a tiny ↑/↓ badge (accent up, muted down).
 */
@Composable
fun EditorialFigure(
    value: String,
    label: String,
    onBg: Color,
    muted: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    delta: Int? = null
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (delta != null && delta != 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "${if (delta > 0) "↑" else "↓"}${kotlin.math.abs(delta)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (delta > 0) accent else muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        FigureLabel(label, muted)
    }
}

/**
 * [EditorialFigure] whose number rolls up from zero the first time it appears — for a figure that
 * lands as a RESULT (the end-of-session readings), never for a resting dashboard total. Same
 * formula, so it sits in a figure row beside plain ones with no seam; [CountUpText] settles on the
 * final value instantly under reduced motion, so nothing is gated on the roll.
 */
@Composable
fun EditorialCountUpFigure(
    value: Double,
    label: String,
    onBg: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    format: (Double) -> String = { it.toInt().toString() }
) {
    Column(modifier) {
        CountUpText(
            value = value,
            style = MaterialTheme.typography.headlineMedium,
            color = onBg,
            fromValue = 0.0,
            format = format
        )
        Spacer(Modifier.height(2.dp))
        FigureLabel(label, muted)
    }
}

/** The mono small-caps caption under a figure — §6's one sanctioned off-scale rung (8-9sp). */
@Composable
private fun FigureLabel(label: String, muted: Color) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = muted, fontSize = 9.sp)
}

/** A dot-plus-caption legend line for charts drawn openly on the page. */
@Composable
fun EditorialLegend(color: Color, label: String, muted: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = muted, fontSize = 8.sp)
    }
}
