package com.coach.screentime.intervention

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.coach.screentime.R
import com.coach.screentime.data.AppRegistry
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.InterventionEntity
import com.coach.screentime.data.store.Mode
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.tracking.NotifChannels
import com.coach.screentime.tracking.SessionAggregator
import com.coach.screentime.util.Time
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes app-open events through the three intervention layers:
 *  - Layer 1: mindfulness pause (5s overlay) on every open of a flagged app
 *  - Layer 2: AI negotiation overlay when daily/category limit crossed
 *  - Layer 3: Ulysses hard lock if user opted in
 *
 * In OBSERVE mode, no overlays fire — only logging.
 */
@Singleton
class InterventionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val rollupDao: RollupDao,
    private val interventionDao: InterventionDao,
    private val settingsStore: SettingsStore,
    private val sessionAggregator: SessionAggregator,
    private val overlayManager: OverlayManager,
    private val appRegistry: AppRegistry,
) {
    private val recentExtensions = mutableMapOf<String, Long>() // pkg -> expiresAt
    private val recentSoftNotifs = mutableMapOf<String, String>() // pkg -> dateLocal soft already fired

    fun start(scope: CoroutineScope) {
        scope.launch {
            sessionAggregator.appOpened.collect { evt ->
                handleOpen(evt.packageName, evt.ts)
            }
        }
    }

    private suspend fun handleOpen(packageName: String, ts: Long) {
        appRegistry.ensureAppRow(packageName)
        val app = appDao.byPackage(packageName) ?: return
        val settings = settingsStore.snapshot()
        if (settings.mode == Mode.OBSERVE) return

        val category = categoryDao.byId(app.categoryId)
        val categoryCapMin = category?.dailyMinutesCap

        // Engage if either the app is individually flagged OR its category has a cap.
        // Without this, setting "Social = 90 min" would do nothing unless every social
        // app was also individually flagged — which surprises users.
        if (!app.isFlagged && categoryCapMin == null) return

        val today = Time.todayString()
        val usedSec = rollupDao.totalSecondsForPackage(today, packageName)
        val perAppCapMin = app.perAppDailyMinutesCap
        val catUsedSec = if (categoryCapMin != null) categoryUsedSec(today, app.categoryId) else 0

        // Layer 3: hard lock has highest priority.
        if (app.hardLockEnabled && perAppCapMin != null && usedSec >= perAppCapMin * 60) {
            log(packageName, "hardlock", "blocked", "perApp", "${perAppCapMin}m")
            overlayManager.showHardLock(packageName, app.displayName)
            return
        }

        // Honor outstanding extension grants.
        val extUntil = recentExtensions[packageName] ?: 0L
        val now = System.currentTimeMillis()
        if (extUntil > now) {
            // User negotiated an extension already. Still apply mindfulness pause though.
            maybeMindfulnessPause(app, usedSec, perAppCapMin, settings.mindfulPauseSec)
            return
        }

        // Layer 2: limit crossed → negotiate.
        val perAppOver = perAppCapMin != null && usedSec >= perAppCapMin * 60
        val categoryOver = categoryCapMin != null && catUsedSec >= categoryCapMin * 60
        if (perAppOver || categoryOver) {
            val triggerKind = if (perAppOver) "perApp" else "category"
            val triggerValue = if (perAppOver) "${perAppCapMin}m" else "${categoryCapMin}m"
            val displayedUsedMin = if (perAppOver) usedSec / 60 else catUsedSec / 60
            val interventionId = log(packageName, "negotiate", "shown", triggerKind, triggerValue)
            overlayManager.showNegotiation(
                packageName = packageName,
                appLabel = app.displayName,
                categoryName = category?.name ?: "Other",
                usedMinutes = displayedUsedMin,
                triggerKind = triggerKind,
                interventionId = interventionId,
            ) { extensionMinutes ->
                recentExtensions[packageName] = System.currentTimeMillis() + extensionMinutes * 60_000L
            }
            return
        }

        // Soft-limit notification at 80% of whichever cap is closest.
        maybeSoftLimitNotif(packageName, app, usedSec, catUsedSec, perAppCapMin, categoryCapMin, today, settings.softLimitPct)

        // Layer 1: mindfulness pause on every open.
        maybeMindfulnessPause(app, usedSec, perAppCapMin, settings.mindfulPauseSec)
    }

    private suspend fun maybeMindfulnessPause(app: AppEntity, usedSec: Int, perAppCapMin: Int?, pauseSec: Int) {
        if (pauseSec <= 0) return
        log(app.packageName, "pause", "shown", "perApp", "${perAppCapMin ?: 0}m")
        overlayManager.showMindfulnessPause(
            packageName = app.packageName,
            appLabel = app.displayName,
            usedMinutes = usedSec / 60,
            pauseSeconds = pauseSec,
        )
    }

    private fun maybeSoftLimitNotif(
        packageName: String,
        app: AppEntity,
        perAppUsedSec: Int,
        categoryUsedSec: Int,
        perAppCapMin: Int?,
        categoryCapMin: Int?,
        date: String,
        softPct: Int,
    ) {
        if (recentSoftNotifs[packageName] == date) return
        val perAppRatio = if (perAppCapMin != null && perAppCapMin > 0) perAppUsedSec.toDouble() / (perAppCapMin * 60) else 0.0
        val categoryRatio = if (categoryCapMin != null && categoryCapMin > 0) categoryUsedSec.toDouble() / (categoryCapMin * 60) else 0.0
        val threshold = softPct / 100.0
        val (kind, ratio, capMin, usedSec) = when {
            perAppRatio >= threshold && perAppRatio >= categoryRatio ->
                Quad("perApp", perAppRatio, perAppCapMin!!, perAppUsedSec)
            categoryRatio >= threshold ->
                Quad("category", categoryRatio, categoryCapMin!!, categoryUsedSec)
            else -> return
        }
        recentSoftNotifs[packageName] = date
        val pct = (ratio * 100).toInt()
        val title = "${app.displayName} — heads up"
        val text = if (kind == "category") {
            "Category total: ${usedSec / 60} of ${capMin} min ($pct%)."
        } else {
            "You're at ${usedSec / 60} of ${capMin} min ($pct%)."
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(context, NotifChannels.INTERVENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(NotifChannels.SOFT_LIMIT_NOTIF_ID + packageName.hashCode(), n)
    }

    private data class Quad(val kind: String, val ratio: Double, val capMin: Int, val usedSec: Int)

    private suspend fun categoryUsedSec(date: String, categoryId: String): Int {
        val packages = appDao.packagesInCategory(categoryId)
        if (packages.isEmpty()) return 0
        return rollupDao.totalSecondsForPackages(date, packages)
    }

    private suspend fun log(
        packageName: String,
        layer: String,
        outcome: String,
        triggerKind: String,
        triggerValue: String,
    ): Long = interventionDao.insert(
        InterventionEntity(
            ts = System.currentTimeMillis(),
            packageName = packageName,
            layer = layer,
            outcome = outcome,
            triggerKind = triggerKind,
            triggerValue = triggerValue,
        )
    )

    fun recordExtension(packageName: String, extensionMinutes: Int) {
        recentExtensions[packageName] = System.currentTimeMillis() + extensionMinutes * 60_000L
    }
}
