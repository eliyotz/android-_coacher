package com.coach.screentime.intervention

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.coach.screentime.R
import com.coach.screentime.data.AppRegistry
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.InterventionEntity
import com.coach.screentime.data.store.Mode
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.focus.FocusActionReceiver
import com.coach.screentime.focus.FocusManager
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
    private val sessionDao: SessionDao,
    private val interventionDao: InterventionDao,
    private val settingsStore: SettingsStore,
    private val sessionAggregator: SessionAggregator,
    private val overlayManager: OverlayManager,
    private val appRegistry: AppRegistry,
    private val focusManager: FocusManager,
) {
    private val recentExtensions = mutableMapOf<String, Long>() // pkg -> expiresAt
    private val recentSoftNotifs = mutableMapOf<String, String>() // pkg -> dateLocal soft already fired
    private val recentCompulsiveNotif = mutableMapOf<String, Long>() // pkg -> last fired at

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

        // Engage if the app is individually flagged OR its category has a cap.
        // Without this, setting "Social = 90 min" would do nothing unless every social
        // app was also individually flagged — which surprises users.
        if (!app.isFlagged && categoryCapMin == null) return

        // Focus mode: hard-lock every flagged app for the duration, no negotiation.
        if (focusManager.isActive(System.currentTimeMillis())) {
            log(packageName, "hardlock", "blocked", "focus", "active")
            overlayManager.showHardLock(packageName, app.displayName)
            return
        }

        val today = Time.todayString()
        val usedSec = rollupDao.totalSecondsForPackage(today, packageName)
        val perAppCapMin = app.perAppDailyMinutesCap
        val catUsedSec = if (categoryCapMin != null) categoryUsedSec(today, app.categoryId) else 0

        // Layer 3: hard lock — including the 24h cool-off when the user has tried to disable it.
        val hardLockEffective = isHardLockEffective(app)
        if (hardLockEffective && perAppCapMin != null && usedSec >= perAppCapMin * 60) {
            log(packageName, "hardlock", "blocked", "perApp", "${perAppCapMin}m")
            overlayManager.showHardLock(packageName, app.displayName)
            return
        }

        // Honor outstanding extension grants.
        val extUntil = recentExtensions[packageName] ?: 0L
        val now = System.currentTimeMillis()
        if (extUntil > now) {
            maybeMindfulnessPause(app, usedSec, perAppCapMin, settings.mindfulPauseSec, escalated = false)
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

        // Compulsive-open check: count opens of this app in the last 30 min.
        // If above threshold, escalate the mindfulness pause and offer a 30-min Focus mode.
        val recentOpens = sessionDao.openCountSince(packageName, now - COMPULSIVE_WINDOW_MS)
        val isCompulsive = recentOpens >= COMPULSIVE_THRESHOLD
        if (isCompulsive) {
            maybeFireCompulsiveNotif(app, recentOpens)
        }

        // Soft-limit notification at 80% of whichever cap is closest.
        maybeSoftLimitNotif(packageName, app, usedSec, catUsedSec, perAppCapMin, categoryCapMin, today, settings.softLimitPct)

        // Layer 1: mindfulness pause on every open. Escalates to 2× length when compulsive.
        maybeMindfulnessPause(app, usedSec, perAppCapMin, settings.mindfulPauseSec, escalated = isCompulsive)
    }

    /**
     * Hard lock is "effective" if either:
     *  - it's currently enabled, OR
     *  - it was enabled and toggled off less than 24 h ago (Ulysses cool-off).
     * The cool-off is tracked via `hardLockToggleAt` in [AppEntity]; the user can
     * toggle off, but the off doesn't take effect until 24 h have elapsed.
     */
    private fun isHardLockEffective(app: AppEntity): Boolean {
        if (app.hardLockEnabled) return true
        if (app.hardLockToggleAt <= 0L) return false
        return System.currentTimeMillis() - app.hardLockToggleAt < HARD_LOCK_COOLOFF_MS
    }

    private suspend fun maybeMindfulnessPause(
        app: AppEntity,
        usedSec: Int,
        perAppCapMin: Int?,
        pauseSec: Int,
        escalated: Boolean,
    ) {
        if (pauseSec <= 0) return
        val effectiveSec = if (escalated) pauseSec * 2 else pauseSec
        log(
            app.packageName, "pause", if (escalated) "escalated" else "shown",
            "perApp", "${perAppCapMin ?: 0}m",
        )
        overlayManager.showMindfulnessPause(
            packageName = app.packageName,
            appLabel = app.displayName,
            usedMinutes = usedSec / 60,
            pauseSeconds = effectiveSec,
        )
    }

    /**
     * Fire at most one compulsive-pattern notification per app per 30 min, offering
     * a one-tap 30-min Focus mode. The notification's action broadcasts to
     * [FocusActionReceiver] which calls [FocusManager.start].
     */
    private fun maybeFireCompulsiveNotif(app: AppEntity, recentOpens: Int) {
        val now = System.currentTimeMillis()
        val last = recentCompulsiveNotif[app.packageName] ?: 0L
        if (now - last < COMPULSIVE_NOTIF_COOLDOWN_MS) return
        recentCompulsiveNotif[app.packageName] = now

        val intent = Intent(context, FocusActionReceiver::class.java).apply {
            action = FocusActionReceiver.ACTION_START
            putExtra(FocusActionReceiver.EXTRA_MINUTES, 30)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            app.packageName.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val nm = context.getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(context, NotifChannels.INTERVENTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${app.displayName} — that's $recentOpens opens in 30 min")
            .setContentText("Looks compulsive. Want a 30-min Focus lock?")
            .addAction(R.drawable.ic_launcher_foreground, "Lock 30 min", pi)
            .setAutoCancel(true)
            .build()
        nm.notify(NotifChannels.COMPULSIVE_NOTIF_ID + app.packageName.hashCode(), n)
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

    companion object {
        private const val COMPULSIVE_WINDOW_MS = 30 * 60_000L
        private const val COMPULSIVE_THRESHOLD = 6
        private const val COMPULSIVE_NOTIF_COOLDOWN_MS = 30 * 60_000L
        const val HARD_LOCK_COOLOFF_MS = 24 * 60 * 60_000L
    }
}
