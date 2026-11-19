package com.forge.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.forge.app.domain.units.WeightUnit

/**
 * App-level UI preferences propagated from MainActivity via [LocalForgeSettings].
 * All screens read from this instead of directly from DataStore so the entire
 * composition re-renders atomically when a setting changes.
 */
data class ForgeUiSettings(
    val amoledMode: Boolean = false,
    /** Weight display unit (GYMAP-72) — lb | kg | st. Read this everywhere weight is shown. */
    val weightUnit: WeightUnit = WeightUnit.LB,
    /** Cardio distance/pace unit — true = miles, false = km. Derives from the weight unit when unset. */
    val useMiles: Boolean = false,
    val hiddenOverviewTiles: Set<String> = emptySet(),
    /** Goals pinned to Home, in pin order. Empty = Home falls back to closest-first. */
    val pinnedGoalKeys: List<String> = emptyList(),
    val overviewTileOrder: List<String> = listOf("gym", "cardio", "trophies"),
    /**
     * 12h/24h for every clock the app draws; read through `clockPattern`. Seeded from the phone's own
     * setting until the user picks one (SettingsRepository.timeFormat24h).
     */
    val timeFormat24h: Boolean = false,
    /** Week starts Monday (true) or Sunday: orders Home's week strip and its weekly counts. */
    val firstDayMonday: Boolean = true,
    val hapticStrength: String = "strong",   // "off" | "light" | "medium" | "strong"
    val keepScreenOn: Boolean = true,        // hold the display awake while logging (GYMAP-74)
    val accentColorHex: String = "",         // empty = AccentNavy default
    val accentEnabled: Boolean = true,       // false = monochrome (neutral highlights, no accent)
    val plateWeightLb: Double = 15.0,        // weight of one plate (lb) for plate-loaded exercises
    /** True once the user has finished a workout — first-touch onboarding cards hide once set. */
    val firstWorkoutDone: Boolean = false
)

val LocalForgeSettings = compositionLocalOf { ForgeUiSettings() }

/**
 * Every field above from ONE settings snapshot, each through the getter its own flow reads, so
 * this and a single flow can never disagree on a default.
 *
 * The app root used to assemble this from fifteen separate flows, starting from `ForgeUiSettings()`:
 * the first composition drew every default (lb, the default accent, a Monday week) and the whole
 * tree recomposed when the real values arrived. From a snapshot the first composition is already
 * the user's own settings, and one collector replaces fifteen.
 */
fun com.forge.app.data.prefs.SettingsRepository.PreferenceValues.toUiSettings() = ForgeUiSettings(
    amoledMode = amoledMode,
    weightUnit = weightUnit,
    useMiles = useMiles,
    hiddenOverviewTiles = hiddenOverviewTiles,
    pinnedGoalKeys = pinnedGoals,
    overviewTileOrder = overviewTileOrder,
    timeFormat24h = timeFormat24h,
    firstDayMonday = firstDayMonday,
    hapticStrength = hapticStrength,
    keepScreenOn = keepScreenOn,
    accentColorHex = effectiveAccentHex,
    accentEnabled = accentEnabled,
    plateWeightLb = plateWeightLb,
    firstWorkoutDone = firstWorkoutDone
)
