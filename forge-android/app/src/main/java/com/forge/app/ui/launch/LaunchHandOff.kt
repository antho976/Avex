package com.forge.app.ui.launch

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.forge.app.appicon.AppIcon

/**
 * The launch: your app icon on the app's own dark, held only until Home is loaded, then taken away
 * by the system. The app animates nothing (Antho, 2026-11-18: "remove the animation, no need for
 * it, I want to jump in as fast as possible"). It used to fade the splash itself as the icon
 * settled a touch smaller (2026-10-21): 240 ms, then 150 ms.
 *
 * What was measured on a Galaxy S21 Ultra, from the launcher starting to dim to Home fully shown
 * (docs/perf/FIXES-2026-11-18.md):
 *
 * - **The system removes the splash, held for Home (this).** About 260 ms; Home starts to show at
 *   about 215. What is left is the system's own short fade, which an app cannot switch off.
 * - **An exit listener that removes the splash at once.** About 240 ms, but the system first hands
 *   its splash view over to the app, and in 2 launches of 20 that showed Home, then the splash
 *   again for one frame, then Home. The same hand-over sat under the old fade.
 * - **No splash at all** (`windowDisablePreview`). Slower: about 525 ms tap to rest, where the
 *   others take about 390. With a splash to show, the launcher starts opening on the tap, while
 *   the app is still starting; without one it waits for the app's first frame.
 *
 * So there is no exit listener: `MainActivity` only sets the splash's keep condition.
 *
 * Where the icon comes from, by platform:
 *
 * - **Android 12+ (API 31).** The system draws the splash before the process starts, from the
 *   launching activity's theme. `Theme.Forge.Splash` (values-v31) names no splash icon, so the
 *   platform draws the launching component's own icon — and a launcher tap starts the enabled
 *   `.icon.*` activity-alias, whose icon IS the pick (an alias's ActivityInfo carries the alias's
 *   icon, not the target's). An activity-alias cannot carry a theme of its own, so naming the icon in
 *   the theme would have pinned every pick to one drawable.
 * - **Android 8–11 (API 26–30).** core-splashscreen's pre-12 splash is the theme's static
 *   `windowBackground`, drawn by the system before any app code runs, so it can only be one drawable
 *   for everyone: it is the dark ground alone (`splash_blank`). The icon is drawn by [LaunchCover]
 *   from the first frame the app draws, and is gone in one frame when Home is ready.
 */

/**
 * The longest the splash is held for Home to finish its first load, counted from the moment Home
 * could have been shown. Home loads in about 50 ms on a Galaxy S21 Ultra; a phone or a history slow
 * enough to pass this gets Home as it is, filling in, which is what every launch used to get.
 */
internal const val HOME_LOAD_HOLD_MS = 250L

/**
 * How long after the first screen is ready a launch counts as settled. Nothing reports the splash
 * gone (there is no exit listener), and the system's own fade is over well inside this. Not a
 * motion duration: the app animates nothing over it.
 */
internal const val LAUNCH_SETTLED_FALLBACK_MS = 150L

/**
 * Called by Home when its first load has finished: today's answer, the coach row and both
 * readings are in. The Activity provides it and holds the splash for it; anywhere else (a
 * preview, a screenshot test) it does nothing.
 */
val LocalOnHomeLoaded = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

/**
 * The icon a launch started from: the `.icon.<Name>` alias the launcher tapped, or null when the app
 * was opened another way (a widget, a notification), which the caller resolves from the enabled
 * alias instead. Pure, so it is pinned by `LaunchHandOffTest`.
 */
internal fun launchIconFromComponent(className: String?): AppIcon? {
    val suffix = className?.substringAfter(".icon.", missingDelimiterValue = "").orEmpty()
    return if (suffix.isEmpty()) null else AppIcon.fromKey(suffix)
}

/** True where the platform splash draws the icon itself (see the file comment). */
internal val systemSplashDrawsIcon: Boolean get() = Build.VERSION.SDK_INT >= 31

/**
 * Android 8–11: the icon on the app's dark, drawn by the app from its first frame, until [ready]
 * (Home is loaded and laid out beneath it). Then it is gone, and says so ([onGone]).
 */
@Composable
fun LaunchCover(icon: AppIcon, ready: Boolean, onGone: () -> Unit = {}) {
    LaunchedEffect(ready) { if (ready) onGone() }
    if (ready) return
    Box(
        Modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon.previewRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
        )
    }
}
