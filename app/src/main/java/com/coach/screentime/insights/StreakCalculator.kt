package com.coach.screentime.insights

import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.RollupDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes the current "all caps respected" streak: number of consecutive
 * past days (excluding today) where every per-app and per-category cap was
 * under-budget. If there are no caps at all, the streak is 0.
 */
@Singleton
class StreakCalculator @Inject constructor(
    private val rollupDao: RollupDao,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
) {
    suspend fun current(maxLookbackDays: Int = 30): Int {
        val apps = appDao.snapshot()
        val categories = categoryDao.snapshot()
        val perAppCaps = apps.filter { it.perAppDailyMinutesCap != null }
        val categoryCaps = categories.filter { it.dailyMinutesCap != null }
        if (perAppCaps.isEmpty() && categoryCaps.isEmpty()) return 0

        val packagesByCategory = apps.groupBy { it.categoryId }.mapValues { it.value.map { a -> a.packageName } }

        var streak = 0
        val iso = DateTimeFormatter.ISO_LOCAL_DATE
        val today = LocalDate.now()
        // Walk backwards starting from yesterday — today is in progress, doesn't count.
        for (i in 1..maxLookbackDays) {
            val date = today.minusDays(i.toLong()).format(iso)
            val rollups = rollupDao.snapshotForDate(date)
            val byPkg = rollups.associateBy { it.packageName }

            val perAppOk = perAppCaps.all { app ->
                val sec = byPkg[app.packageName]?.totalSec ?: 0
                sec <= (app.perAppDailyMinutesCap ?: Int.MAX_VALUE) * 60
            }
            val categoryOk = categoryCaps.all { c ->
                val pkgs = packagesByCategory[c.id].orEmpty()
                val sec = pkgs.sumOf { byPkg[it]?.totalSec ?: 0 }
                sec <= (c.dailyMinutesCap ?: Int.MAX_VALUE) * 60
            }
            if (perAppOk && categoryOk) streak += 1 else break
        }
        return streak
    }
}
