package com.coach.screentime.tracking

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polls UsageStatsManager every ~2s as a fallback for AccessibilityService.
 * Doubles as the source of truth on cold start (queries the last 30s of events
 * to figure out what's already in the foreground when the service boots).
 */
@Singleton
class UsageStatsPoller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val currentAppTracker: CurrentAppTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // Initial backfill: figure out what's in foreground right now.
            backfillRecent()
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                pollLatest()
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    private fun usageManager(): UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun hasPermission(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun backfillRecent() {
        if (!hasPermission()) return
        val now = System.currentTimeMillis()
        val pkg = lastForegroundPackage(now - BACKFILL_WINDOW_MS, now)
        if (pkg != null) {
            currentAppTracker.update(pkg, now, CurrentAppTracker.Source.POLLER)
        }
    }

    private fun pollLatest() {
        if (!hasPermission()) return
        val now = System.currentTimeMillis()
        val pkg = lastForegroundPackage(now - POLL_WINDOW_MS, now) ?: return
        currentAppTracker.update(pkg, now, CurrentAppTracker.Source.POLLER)
    }

    private fun lastForegroundPackage(start: Long, end: Long): String? {
        val mgr = usageManager() ?: return null
        val events = mgr.queryEvents(start, end)
        val ev = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                last = ev.packageName
            }
        }
        return last
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2_000L
        private const val POLL_WINDOW_MS = 10_000L
        private const val BACKFILL_WINDOW_MS = 60_000L
    }
}
