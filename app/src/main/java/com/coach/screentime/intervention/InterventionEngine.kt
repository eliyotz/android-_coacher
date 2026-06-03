package com.coach.screentime.intervention

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
import com.coach.screentime.focus.FocusManager
import com.coach.screentime.punishment.PunishmentManager
import com.coach.screentime.tracking.SessionAggregator
import com.coach.screentime.util.Time
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
    private val punishmentManager: PunishmentManager,
) {
    private val recentExtensions = mutableMapOf<String, Long>() // pkg -> expiresAt

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

        val now = System.currentTimeMillis()

        // Punishments take precedence over everything except focus mode. The AI
        // can target any app, even one the user hasn't individually flagged.
        val activeBlock = punishmentManager.isAppBlocked(packageName, now)
        if (activeBlock != null) {
            val mins = ((activeBlock.punishment.expiresAt - now) / 60_000L).coerceAtLeast(0L).toInt()
            log(packageName, "punishment", "blocked", "ai", "${mins}m")
            overlayManager.showHardLock(packageName, app.displayName)
            return
        }

        val category = categoryDao.byId(app.categoryId)
        val categoryCapMin = category?.dailyMinutesCap

        // Engage if the app is individually flagged OR its category has a cap.
        // Without this, setting "Social = 90 min" would do nothing unless every social
        // app was also individually flagged — which surprises users.
        if (!app.isFlagged && categoryCapMin == null) return

        // Focus mode: hard-lock every flagged app for the duration, no negotiation.
        if (focusManager.isActive(now)) {
            log(packageName, "hardlock", "blocked", "focus", "active")
            overlayManager.showHardLock(packageName, app.displayName)
            return
        }

        val today = Time.todayString()
        val usedSec = rollupDao.totalSecondsForPackage(today, packageName)

        // Active punishment can shrink today's caps by a percentage.
        val capReductionPct = punishmentManager.capReductionPct(now)
        val perAppCapMin = app.perAppDailyMinutesCap?.let { applyReduction(it, capReductionPct) }
        val effectiveCategoryCapMin = categoryCapMin?.let { applyReduction(it, capReductionPct) }
        val catUsedSec = if (effectiveCategoryCapMin != null) categoryUsedSec(today, app.categoryId) else 0

        // Layer 3: hard lock — including the 24h cool-off when the user has tried to disable it.
        val hardLockEffective = isHardLockEffective(app)
        if (hardLockEffective && perAppCapMin != null && usedSec >= perAppCapMin * 60) {
            log(packageName, "hardlock", "blocked", "perApp", "${perAppCapMin}m")
            overlayManager.showHardLock(packageName, app.displayName)
            return
        }

        // Honor outstanding extension grants.
        val extUntil = recentExtensions[packageName] ?: 0L
        val pauseMultiplier = punishmentManager.mindfulPauseMultiplier(now)
        if (extUntil > now) {
            maybeMindfulnessPause(app, usedSec, perAppCapMin, settings.mindfulPauseSec, escalated = false, multiplier = pauseMultiplier)
            return
        }

        // Layer 2: limit crossed → negotiate.
        val perAppOver = perAppCapMin != null && usedSec >= perAppCapMin * 60
        val categoryOver = effectiveCategoryCapMin != null && catUsedSec >= effectiveCategoryCapMin * 60
        if (perAppOver || categoryOver) {
            val triggerKind = if (perAppOver) "perApp" else "category"
            val triggerValue = if (perAppOver) "${perAppCapMin}m" else "${effectiveCategoryCapMin}m"
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

        // Compulsive-open check: count opens of this app in the last 30 min. Above the
        // threshold we lengthen the mindfulness pause — a quiet, in-the-moment signal.
        // We deliberately do NOT fire a separate "looks compulsive" notification, nor a
        // soft-limit "you're at 80%" notification: the product mandate is zero notification
        // spam. The pause itself is the only in-the-moment surface.
        val recentOpens = sessionDao.openCountSince(packageName, now - COMPULSIVE_WINDOW_MS)
        val isCompulsive = recentOpens >= COMPULSIVE_THRESHOLD

        // Layer 1: mindfulness pause on every open. Escalates with compulsive use, plus
        // any active punishment can multiply the pause length.
        maybeMindfulnessPause(app, usedSec, perAppCapMin, settings.mindfulPauseSec, escalated = isCompulsive, multiplier = pauseMultiplier)
    }

    private fun applyReduction(capMin: Int, reductionPct: Int): Int =
        if (reductionPct <= 0) capMin else (capMin * (100 - reductionPct) / 100).coerceAtLeast(1)

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
        multiplier: Float = 1f,
    ) {
        if (pauseSec <= 0) return
        val compulsiveBonus = if (escalated) 2f else 1f
        val effectiveSec = (pauseSec * compulsiveBonus * multiplier).toInt().coerceAtLeast(pauseSec)
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
        const val HARD_LOCK_COOLOFF_MS = 24 * 60 * 60_000L
    }
}
