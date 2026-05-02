package com.coach.screentime.data.store

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "coach_settings")

enum class Strictness { GENTLE, BALANCED, STRICT }
enum class Mode { OBSERVE, ENFORCE }

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds = context.dataStore

    object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val OBSERVE_START = longPreferencesKey("observe_start_ts")
        val MODE = stringPreferencesKey("mode")
        val STRICTNESS = stringPreferencesKey("strictness")
        val MINDFUL_PAUSE_SEC = intPreferencesKey("mindful_pause_sec")
        val SOFT_LIMIT_PCT = intPreferencesKey("soft_limit_pct")
        val EXTENSION_MINUTES = intPreferencesKey("extension_minutes")
        val USER_GOAL = stringPreferencesKey("user_goal")
        val LAST_WEEKLY_REPORT_AT = longPreferencesKey("last_weekly_report_at")
        val FOCUS_END_TS = longPreferencesKey("focus_end_ts")
        val NUDGES_ENABLED = booleanPreferencesKey("nudges_enabled")
        val REFLECTION_ENABLED = booleanPreferencesKey("reflection_enabled")
    }

    val onboarded: Flow<Boolean> = ds.data.map { it[Keys.ONBOARDED] ?: false }
    val observeStartTs: Flow<Long> = ds.data.map { it[Keys.OBSERVE_START] ?: 0L }
    val mode: Flow<Mode> = ds.data.map { Mode.valueOf(it[Keys.MODE] ?: Mode.OBSERVE.name) }
    val strictness: Flow<Strictness> = ds.data.map { Strictness.valueOf(it[Keys.STRICTNESS] ?: Strictness.BALANCED.name) }
    val mindfulPauseSec: Flow<Int> = ds.data.map { it[Keys.MINDFUL_PAUSE_SEC] ?: 5 }
    val softLimitPct: Flow<Int> = ds.data.map { it[Keys.SOFT_LIMIT_PCT] ?: 80 }
    val extensionMinutes: Flow<Int> = ds.data.map { it[Keys.EXTENSION_MINUTES] ?: 15 }
    val userGoal: Flow<String> = ds.data.map { it[Keys.USER_GOAL] ?: "" }
    val focusEndTs: Flow<Long> = ds.data.map { it[Keys.FOCUS_END_TS] ?: 0L }
    val nudgesEnabled: Flow<Boolean> = ds.data.map { it[Keys.NUDGES_ENABLED] ?: true }
    val reflectionEnabled: Flow<Boolean> = ds.data.map { it[Keys.REFLECTION_ENABLED] ?: true }

    suspend fun snapshot(): Snapshot {
        val p: Preferences = ds.data.first()
        return Snapshot(
            onboarded = p[Keys.ONBOARDED] ?: false,
            mode = Mode.valueOf(p[Keys.MODE] ?: Mode.OBSERVE.name),
            strictness = Strictness.valueOf(p[Keys.STRICTNESS] ?: Strictness.BALANCED.name),
            mindfulPauseSec = p[Keys.MINDFUL_PAUSE_SEC] ?: 5,
            softLimitPct = p[Keys.SOFT_LIMIT_PCT] ?: 80,
            extensionMinutes = p[Keys.EXTENSION_MINUTES] ?: 15,
            userGoal = p[Keys.USER_GOAL] ?: "",
            observeStartTs = p[Keys.OBSERVE_START] ?: 0L,
            lastWeeklyReportAt = p[Keys.LAST_WEEKLY_REPORT_AT] ?: 0L,
            focusEndTs = p[Keys.FOCUS_END_TS] ?: 0L,
            nudgesEnabled = p[Keys.NUDGES_ENABLED] ?: true,
            reflectionEnabled = p[Keys.REFLECTION_ENABLED] ?: true,
        )
    }

    suspend fun setOnboarded(value: Boolean) = ds.edit { it[Keys.ONBOARDED] = value }
    suspend fun setMode(value: Mode) = ds.edit { it[Keys.MODE] = value.name }
    suspend fun setStrictness(value: Strictness) = ds.edit { it[Keys.STRICTNESS] = value.name }
    suspend fun setMindfulPauseSec(value: Int) = ds.edit { it[Keys.MINDFUL_PAUSE_SEC] = value }
    suspend fun setSoftLimitPct(value: Int) = ds.edit { it[Keys.SOFT_LIMIT_PCT] = value }
    suspend fun setExtensionMinutes(value: Int) = ds.edit { it[Keys.EXTENSION_MINUTES] = value }
    suspend fun setUserGoal(value: String) = ds.edit { it[Keys.USER_GOAL] = value }
    suspend fun setObserveStart(ts: Long) = ds.edit { it[Keys.OBSERVE_START] = ts }
    suspend fun setLastWeeklyReportAt(ts: Long) = ds.edit { it[Keys.LAST_WEEKLY_REPORT_AT] = ts }
    suspend fun setFocusEndTs(ts: Long) = ds.edit { it[Keys.FOCUS_END_TS] = ts }
    suspend fun setNudgesEnabled(value: Boolean) = ds.edit { it[Keys.NUDGES_ENABLED] = value }
    suspend fun setReflectionEnabled(value: Boolean) = ds.edit { it[Keys.REFLECTION_ENABLED] = value }

    data class Snapshot(
        val onboarded: Boolean,
        val mode: Mode,
        val strictness: Strictness,
        val mindfulPauseSec: Int,
        val softLimitPct: Int,
        val extensionMinutes: Int,
        val userGoal: String,
        val observeStartTs: Long,
        val lastWeeklyReportAt: Long,
        val focusEndTs: Long,
        val nudgesEnabled: Boolean,
        val reflectionEnabled: Boolean,
    )
}
