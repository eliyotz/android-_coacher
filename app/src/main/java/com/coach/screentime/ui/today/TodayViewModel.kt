package com.coach.screentime.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.PunishmentDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import com.coach.screentime.data.db.entities.DailyRollupEntity
import com.coach.screentime.data.db.entities.PunishmentEntity
import com.coach.screentime.insights.StreakCalculator
import com.coach.screentime.util.Time
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val rollupDao: RollupDao,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val sessionDao: SessionDao,
    private val punishmentDao: PunishmentDao,
    private val streakCalculator: StreakCalculator,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = combine(
        rollupDao.observeForDate(Time.todayString()),
        appDao.observeAll(),
        categoryDao.observeAll(),
    ) { rollups, apps, categories ->
        buildUiState(rollups, apps, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Empty)

    private suspend fun buildUiState(
        rollups: List<DailyRollupEntity>,
        apps: List<AppEntity>,
        categories: List<CategoryEntity>,
    ): TodayUiState {
        val appsByPkg = apps.associateBy { it.packageName }
        val rows = rollups
            .filter { (appsByPkg[it.packageName])?.isFlagged == true || it.totalSec >= 60 }
            .sortedByDescending { it.totalSec }
            .take(15)
            .map { r ->
                val app = appsByPkg[r.packageName]
                AppUsageRow(
                    packageName = r.packageName,
                    label = app?.displayName ?: r.packageName,
                    minutes = r.totalSec / 60,
                    opens = r.opensCount,
                    capMinutes = app?.perAppDailyMinutesCap,
                )
            }

        val totalMinutes = rollups.sumOf { it.totalSec } / 60
        val totalOpens = rollups.sumOf { it.opensCount }

        // Adherence proxy: fraction of sessions under 10 min on flagged apps.
        val flaggedRollups = rollups.filter { (appsByPkg[it.packageName])?.isFlagged == true }
        val healthyOpens = flaggedRollups.sumOf {
            val avg = if (it.opensCount > 0) it.totalSec / it.opensCount else 0
            if (avg < 600) it.opensCount else 0
        }
        val flaggedOpens = flaggedRollups.sumOf { it.opensCount }
        val adherence = if (flaggedOpens > 0) healthyOpens.toFloat() / flaggedOpens else 1f

        val rollupsByCategory = rollups.groupBy { appsByPkg[it.packageName]?.categoryId ?: "other" }
        val categoryRows = categories.map { c ->
            val rs = rollupsByCategory[c.id].orEmpty()
            CategoryRow(
                id = c.id,
                name = c.name,
                minutes = rs.sumOf { it.totalSec } / 60,
                opens = rs.sumOf { it.opensCount },
                capMinutes = c.dailyMinutesCap,
            )
        }.sortedByDescending { it.minutes }

        val streak = streakCalculator.current()
        val worstHourLabel = computeWorstHour()
        val activePunishment = punishmentDao.activeSnapshot(System.currentTimeMillis()).firstOrNull()
        val sevenDayAvgMinByPkg = sevenDayAverages(rows.map { it.packageName })
        val rowsWithDelta = rows.map { r ->
            val avg = sevenDayAvgMinByPkg[r.packageName] ?: 0
            val deltaPct = if (avg > 0) ((r.minutes - avg) * 100) / avg else null
            r.copy(deltaVsWeekAvgPct = deltaPct)
        }

        return TodayUiState(
            totalMinutes = totalMinutes,
            totalOpens = totalOpens,
            adherence = adherence,
            apps = rowsWithDelta,
            categories = categoryRows,
            streak = streak,
            worstHourLabel = worstHourLabel,
            activePunishment = activePunishment,
        )
    }

    private suspend fun computeWorstHour(): String? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val sessions = sessionDao.forDate(today)
        if (sessions.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val byHour = IntArray(24)
        sessions.forEach { s ->
            val hour = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(s.startTs), zone).hour
            byHour[hour] = byHour[hour] + s.durationSec
        }
        val worstHour = byHour.indices.maxByOrNull { byHour[it] } ?: return null
        if (byHour[worstHour] < 60) return null
        val mins = byHour[worstHour] / 60
        val hh = worstHour.toString().padStart(2, '0')
        return "${hh}:00 — ${mins}m"
    }

    private suspend fun sevenDayAverages(packages: List<String>): Map<String, Int> {
        if (packages.isEmpty()) return emptyMap()
        // Average over the last 7 days (excluding today).
        val today = LocalDate.now()
        val iso = DateTimeFormatter.ISO_LOCAL_DATE
        val dates = (1..7).map { today.minusDays(it.toLong()).format(iso) }
        val rollups = rollupDao.snapshotForDates(dates)
        return packages.associateWith { pkg ->
            val total = rollups.filter { it.packageName == pkg }.sumOf { it.totalSec }
            (total / 60) / 7
        }
    }
}

data class TodayUiState(
    val totalMinutes: Int,
    val totalOpens: Int,
    val adherence: Float,
    val apps: List<AppUsageRow>,
    val categories: List<CategoryRow>,
    val streak: Int = 0,
    val worstHourLabel: String? = null,
    val activePunishment: PunishmentEntity? = null,
) {
    companion object { val Empty = TodayUiState(0, 0, 1f, emptyList(), emptyList()) }
}

data class AppUsageRow(
    val packageName: String,
    val label: String,
    val minutes: Int,
    val opens: Int,
    val capMinutes: Int?,
    val deltaVsWeekAvgPct: Int? = null,
)

data class CategoryRow(
    val id: String,
    val name: String,
    val minutes: Int,
    val opens: Int,
    val capMinutes: Int?,
)
