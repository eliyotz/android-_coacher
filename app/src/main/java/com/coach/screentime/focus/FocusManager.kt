package com.coach.screentime.focus

import com.coach.screentime.data.store.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates Focus mode — a user-initiated lock that hard-locks every flagged
 * app for a fixed duration without negotiation. Bypasses the AI entirely.
 *
 * Persisted as a single `focusEndTs` in [SettingsStore]. If `focusEndTs > now`,
 * focus mode is active.
 */
@Singleton
class FocusManager @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    suspend fun isActive(now: Long = System.currentTimeMillis()): Boolean {
        val end = settingsStore.snapshot().focusEndTs
        return end > now
    }

    suspend fun activeUntil(): Long = settingsStore.snapshot().focusEndTs

    suspend fun start(durationMinutes: Int) {
        val until = System.currentTimeMillis() + durationMinutes * 60_000L
        settingsStore.setFocusEndTs(until)
    }

    suspend fun stop() {
        settingsStore.setFocusEndTs(0L)
    }

    /** Cycles 30m → 60m → off, used by the Quick Settings tile. */
    suspend fun cycle(): State {
        val now = System.currentTimeMillis()
        val end = settingsStore.snapshot().focusEndTs
        val remainingMs = end - now
        return when {
            remainingMs <= 0 -> { start(30); State.Active(30) }
            remainingMs <= 30 * 60_000L -> { start(60); State.Active(60) }
            else -> { stop(); State.Off }
        }
    }

    sealed interface State {
        data object Off : State
        data class Active(val minutes: Int) : State
    }
}
