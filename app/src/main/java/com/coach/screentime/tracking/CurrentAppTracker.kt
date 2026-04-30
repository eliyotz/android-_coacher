package com.coach.screentime.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "what app is in the foreground right now".
 * Both AccessibilityService (fast path) and UsageStatsPoller (fallback) write here.
 * The InterventionEngine and SessionAggregator both read from this flow.
 */
@Singleton
class CurrentAppTracker @Inject constructor() {
    private val _state = MutableStateFlow(State("", 0L, source = Source.NONE))
    val state: StateFlow<State> = _state.asStateFlow()

    fun update(packageName: String, ts: Long, source: Source) {
        val current = _state.value
        if (packageName.isBlank()) return
        // Allow accessibility (fast) to overwrite poller; ignore poller if accessibility just spoke.
        if (current.packageName == packageName && current.source.priority >= source.priority) {
            // Same app, same-or-better source; nothing to do.
            return
        }
        _state.value = State(packageName, ts, source)
    }

    data class State(val packageName: String, val ts: Long, val source: Source)
    enum class Source(val priority: Int) { NONE(-1), POLLER(0), ACCESSIBILITY(1) }
}
