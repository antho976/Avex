package com.forge.app.ui.gym.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.app.data.db.dao.SessionDao
import com.forge.app.data.repo.AdaptationRepository
import com.forge.app.data.repo.StatsRepository
import com.forge.app.ui.gym.stats.state.StatsUiState
import com.forge.app.ui.gym.stats.state.balanceRatioUi
import com.forge.app.ui.gym.stats.state.buildReadinessPulse
import com.forge.app.ui.gym.stats.state.plateauFlagOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepo: StatsRepository,
    adaptationRepo: AdaptationRepository,
    private val settingsRepo: com.forge.app.data.prefs.SettingsRepository,
    private val sessionDao: SessionDao
) : ViewModel() {

    /**
     * The engine read is a whole-history snapshot fan-out. Re-run it whenever the set of FINISHED
     * sessions changes — a session finishes, or "Reset session data" / a delete wipes them — by
     * keying off the finished-session COUNT, NOT the per-set reactive combine below, so it still
     * can't join the per-set hot path (logging sets in an in-progress session leaves the count,
     * and so this flow, untouched). A one-shot init load instead left the always-on balance ratios
     * (push/pull, quad/ham) stale on the retained ViewModel after a reset — GYMAP-18. runCatching
     * degrades a snapshot failure to null (no pulse/plateaus/insights) rather than crashing.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val engineFlow: StateFlow<AdaptationRepository.EngineStatsRead?> =
        sessionDao.observeFinishedCount()
            .mapLatest {
                // Degrade a real snapshot failure to null (no pulse/plateaus/insights), but let a
                // mapLatest cancellation propagate — swallowing it could emit a stale null.
                runCatching { adaptationRepo.engineStatsRead() }
                    .getOrElse { if (it is kotlinx.coroutines.CancellationException) throw it else null }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Last Stats LENS the user settled on (by enum NAME), persisted so reopening Stats lands there
     *  (reuses the old sub-tab pref; retired tab names simply fall back to the default). Maps an
     *  unset pref to the default so emitted values are always non-null — the screen reserves null
     *  for "not yet loaded" and only deep-links once a real value arrives. */
    val lastStatsTabName: StateFlow<String?> =
        settingsRepo.lastStatsTabName
            .map { it ?: StatsLens.STRONGER.name }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveStatsTab(name: String) = viewModelScope.launch { settingsRepo.setLastStatsTabName(name) }

    val state: StateFlow<StatsUiState> = combine(
        statsRepo.observeGymStats(),
        engineFlow,
        settingsRepo.userSex
    ) { snapshot, engine, sex ->
        StatsUiState(
            isLoading = false,
            weekComparison = snapshot.weekComparison,
            overload = snapshot.overload,

            consistencyStreak = snapshot.consistencyStreak,
            weeklySessionCounts = snapshot.weeklySessionCounts,
            weeklyDurations = snapshot.weeklyDurations,
            exerciseFrequency = snapshot.exerciseFrequency,

            e1rmLifts = snapshot.e1rmLifts,
            recentPrs = snapshot.recentPrs,
            strengthCurves = snapshot.strengthCurves,
            plateauFlags = engine?.plateaus.orEmpty().mapNotNull(::plateauFlagOf),
            prRecency = snapshot.prRecency,
            timeToPr = snapshot.timeToPr,
            bodyweightPoints = snapshot.bodyweightPoints,
            userSex = sex,
            repMaxes = snapshot.repMaxes,
            patternAxes = snapshot.patternAxes,

            weeklySetsByMuscle = snapshot.weeklySetsByMuscle,
            plannedSetsByMuscle = snapshot.plannedSetsByMuscle,
            repRange = snapshot.repRange,
            balanceRatios = engine?.ratios.orEmpty().map(::balanceRatioUi),
            weeklyTonnage = snapshot.weeklyTonnage,
            dayTypeVolume = snapshot.dayTypeVolume,

            readinessPulse = engine?.fatigue?.let { buildReadinessPulse(it, engine.deloadScoreThreshold) },
            readinessThreshold = engine?.deloadScoreThreshold,
            rpeDistribution = snapshot.rpeDistribution,
            avgRpe = snapshot.avgRpe,
            avgRpePerSession = snapshot.avgRpePerSession,
            weeklyEffort = snapshot.weeklyEffort,
            dailyActivity = snapshot.dailyActivity
        )
    }.catch {
        // A crash in any stats aggregation drops to a non-loading ERROR state instead of an
        // infinite spinner OR a silent empty (which would read as "no data yet"). The screen
        // shows a distinct "couldn't load your stats" message (E4).
        emit(StatsUiState(isLoading = false, loadError = true))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = StatsUiState()
    )
}
