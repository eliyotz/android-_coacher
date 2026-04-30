package com.coach.screentime.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import com.coach.screentime.data.db.entities.DailyRollupEntity
import com.coach.screentime.util.Time
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val rollupDao: RollupDao,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = combine(
        rollupDao.observeForDate(Time.todayString()),
        appDao.observeAll(),
        categoryDao.observeAll(),
    ) { rollups, apps, categories ->
        buildUiState(rollups, apps, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Empty)

    private fun buildUiState(
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

        val byCategory = rollups.groupBy { appsByPkg[it.packageName]?.categoryId ?: "other" }
            .mapValues { (_, rs) -> rs.sumOf { it.totalSec } }
        val categoryRows = categories.map { c ->
            CategoryRow(
                id = c.id,
                name = c.name,
                minutes = (byCategory[c.id] ?: 0) / 60,
                capMinutes = c.dailyMinutesCap,
            )
        }.sortedByDescending { it.minutes }

        return TodayUiState(
            totalMinutes = totalMinutes,
            totalOpens = totalOpens,
            adherence = adherence,
            apps = rows,
            categories = categoryRows,
        )
    }
}

data class TodayUiState(
    val totalMinutes: Int,
    val totalOpens: Int,
    val adherence: Float,
    val apps: List<AppUsageRow>,
    val categories: List<CategoryRow>,
) {
    companion object { val Empty = TodayUiState(0, 0, 1f, emptyList(), emptyList()) }
}

data class AppUsageRow(
    val packageName: String,
    val label: String,
    val minutes: Int,
    val opens: Int,
    val capMinutes: Int?,
)

data class CategoryRow(
    val id: String,
    val name: String,
    val minutes: Int,
    val capMinutes: Int?,
)
