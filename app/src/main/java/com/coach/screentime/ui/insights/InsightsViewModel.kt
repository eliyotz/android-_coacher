package com.coach.screentime.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.entities.WeeklyReportEntity
import com.coach.screentime.util.Time
import com.coach.screentime.work.WeeklyReportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportDao: ReportDao,
    private val rollupDao: RollupDao,
    private val appDao: AppDao,
) : ViewModel() {

    val state: StateFlow<InsightsUiState> = combine(
        reportDao.observeAll(),
        appDao.observeAll(),
    ) { reports, _ ->
        val days = Time.lastNDaysStrings(7)
        val rollupRows = rollupDao.snapshotForDates(days)
        val byDate = days.reversed().map { date ->
            val total = rollupRows.filter { it.dateLocal == date }.sumOf { it.totalSec } / 60
            DayBar(date = date, minutes = total)
        }
        InsightsUiState(
            reports = reports,
            sevenDayBars = byDate,
            empty = reports.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState.Empty)

    fun runReportNow() {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<WeeklyReportWorker>().build()
        )
    }
}

data class InsightsUiState(
    val reports: List<WeeklyReportEntity>,
    val sevenDayBars: List<DayBar>,
    val empty: Boolean,
) {
    companion object { val Empty = InsightsUiState(emptyList(), emptyList(), true) }
}

data class DayBar(val date: String, val minutes: Int)
