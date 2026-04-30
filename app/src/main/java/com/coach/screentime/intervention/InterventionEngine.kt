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
        if (!app.isFlagged) return

        val today = Time.todayString()
        val usedSec = rollupDao.totalSecondsForPackage(today, packageName)
        val perAppCapMin = app.perAppDailyMinutesCap
        val category = categoryDao.byId(app.categoryId)
        val categoryCapMin = category?.dailyMinutesCap

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
        val categoryOver = categoryCapMin != null && categoryUsedSec(today, app.categoryId) >= categoryCapMin * 60
        if (perAppOver || categoryOver) {
            val triggerKind = if (perAppOver) "perApp" else "category"
            val triggerValue = if (perAppOver) "${perAppCapMin}m" else "${categoryCapMin}m"
            val interventionId = log(packageName, "negotiate", "shown", triggerKind, triggerValue)
            overlayManager.showNegotiation(
                packageName = packageName,
                appLabel = app.displayName,
                categoryName = category?.name ?: "Other",
                usedMinutes = usedSec / 60,
                triggerKind = triggerKind,
                interventionId = interventionId,
            ) { extensionMinutes ->
                recentExtensions[packageName] = System.currentTimeMillis() + extensionMinutes * 60_000L
            }
            return
        }

        // Soft-limit notification at 80%.
        maybeSoftLimitNotif(packageName, app, usedSec, perAppCapMin, categoryCapMin, today, settings.softLimitPct)

        // Layer 1: mindfulness pause on every open of a flagged app.
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

    private suspend fun maybeSoftLimitNotif(
        packageName: String,
        app: AppEntity,
        usedSec: Int,
        perAppCapMin: Int?,
        categoryCapMin: Int?,
        date: String,
        softPct: Int,
    ) {
        if (recentSoftNotifs[packageName] == date) return
        val capMin = perAppCapMin ?: categoryCapMin ?: return
        val threshold = capMin * 60 * softPct / 100
        if (usedSec < threshold) return
        recentSoftNotifs[packageName] = date
        val nm = context.getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(context, NotifChannels.INTERVENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${app.displayName} — heads up")
            .setContentText("You're at ${usedSec / 60} min of ${capMin} min today.")
            .setAutoCancel(true)
            .build()
        nm.notify(NotifChannels.SOFT_LIMIT_NOTIF_ID + packageName.hashCode(), n)
    }

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
