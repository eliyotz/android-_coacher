package com.coach.screentime.tracking

import com.coach.screentime.data.AppRegistry
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.entities.DailyRollupEntity
import com.coach.screentime.data.db.entities.SessionEntity
import com.coach.screentime.util.Time
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Coalesces foreground-app changes into sessions. A session starts when an app
 * comes to foreground and ends when another app (or "system") takes over, or
 * when the screen turns off.
 *
 * Sessions shorter than [minSessionSec] are still recorded as opens but contribute
 * almost no time — that's important: those are the compulsive checks we care about.
 */
@Singleton
class SessionAggregator @Inject constructor(
    private val sessionDao: SessionDao,
    private val rollupDao: RollupDao,
    private val appRegistry: AppRegistry,
) {
    private val mutex = Mutex()
    private var current: Open? = null

    private val _sessionEnded = MutableSharedFlow<EndedSession>(extraBufferCapacity = 16)
    val sessionEnded: SharedFlow<EndedSession> = _sessionEnded.asSharedFlow()

    private val _appOpened = MutableSharedFlow<OpenedApp>(extraBufferCapacity = 16)
    val appOpened: SharedFlow<OpenedApp> = _appOpened.asSharedFlow()

    suspend fun onForegroundAppChanged(packageName: String, ts: Long) = mutex.withLock {
        val cur = current
        if (cur != null && cur.packageName == packageName) return@withLock
        if (cur != null) closeOpen(cur, ts)
        if (packageName.isNotBlank() && !isSystemPackage(packageName)) {
            current = Open(packageName, ts)
            appRegistry.ensureAppRow(packageName)
            _appOpened.tryEmit(OpenedApp(packageName, ts))
        } else {
            current = null
        }
    }

    suspend fun onScreenOff(ts: Long) = mutex.withLock {
        val cur = current ?: return@withLock
        closeOpen(cur, ts)
        current = null
    }

    private suspend fun closeOpen(open: Open, ts: Long) {
        val durationSec = max(0, ((ts - open.startTs) / 1000L).toInt())
        if (durationSec < MIN_PERSIST_SEC && durationSec > 0) {
            // Still emit "ended" so observers can react, but do not bother persisting micro-sessions.
            _sessionEnded.tryEmit(EndedSession(open.packageName, open.startTs, ts, durationSec))
            return
        }
        val date = Time.localDateString(open.startTs)
        sessionDao.insert(
            SessionEntity(
                packageName = open.packageName,
                startTs = open.startTs,
                endTs = ts,
                durationSec = durationSec,
                dateLocal = date,
            )
        )
        rollupTouch(date, open.packageName, durationSec)
        _sessionEnded.tryEmit(EndedSession(open.packageName, open.startTs, ts, durationSec))
    }

    /** Increment open count and add session time to today's rollup. Idempotent on (date, pkg). */
    private suspend fun rollupTouch(date: String, pkg: String, durationSec: Int) {
        val existing = rollupDao.snapshotForDate(date).firstOrNull { it.packageName == pkg }
        val updated = if (existing == null) {
            DailyRollupEntity(date, pkg, durationSec, 1, durationSec)
        } else {
            existing.copy(
                totalSec = existing.totalSec + durationSec,
                opensCount = existing.opensCount + 1,
                longestSessionSec = max(existing.longestSessionSec, durationSec),
            )
        }
        rollupDao.upsertAll(listOf(updated))
    }

    private fun isSystemPackage(pkg: String): Boolean = pkg in IGNORED_PACKAGES

    data class Open(val packageName: String, val startTs: Long)
    data class EndedSession(val packageName: String, val startTs: Long, val endTs: Long, val durationSec: Int)
    data class OpenedApp(val packageName: String, val ts: Long)

    companion object {
        private const val MIN_PERSIST_SEC = 2
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.launcher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oneplus.launcher",
        )
    }
}
