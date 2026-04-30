package com.coach.screentime.tracking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Real-time foreground-app detection via WINDOW_STATE_CHANGED.
 * This is the fast path; UsageStatsPoller is the safety net if the OS kills us.
 *
 * We deliberately do NOT read window content (canRetrieveWindowContent=false in
 * the XML config). The package name on the event is enough.
 */
@AndroidEntryPoint
class UsageAccessibilityService : AccessibilityService() {

    @Inject lateinit var currentAppTracker: CurrentAppTracker

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg.isBlank() || pkg == packageName) return
        currentAppTracker.update(pkg, System.currentTimeMillis(), CurrentAppTracker.Source.ACCESSIBILITY)
    }

    override fun onInterrupt() = Unit
}
