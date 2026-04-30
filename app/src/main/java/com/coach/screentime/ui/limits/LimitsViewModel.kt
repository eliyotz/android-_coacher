package com.coach.screentime.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.AppRegistry
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LimitsViewModel @Inject constructor(
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val appRegistry: AppRegistry,
) : ViewModel() {

    init { viewModelScope.launch { appRegistry.refreshInstalledApps() } }

    val state: StateFlow<LimitsUiState> = combine(
        appDao.observeAll(),
        categoryDao.observeAll(),
    ) { apps, categories -> LimitsUiState(apps, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LimitsUiState.Empty)

    fun setFlag(packageName: String, flagged: Boolean) = viewModelScope.launch {
        val a = appDao.byPackage(packageName) ?: return@launch
        appDao.update(a.copy(isFlagged = flagged))
    }

    fun setPerAppCap(packageName: String, minutes: Int?) = viewModelScope.launch {
        val a = appDao.byPackage(packageName) ?: return@launch
        appDao.update(a.copy(perAppDailyMinutesCap = minutes))
    }

    fun setHardLock(packageName: String, enabled: Boolean) = viewModelScope.launch {
        val a = appDao.byPackage(packageName) ?: return@launch
        // Enabling is immediate. Disabling has a 24h cool-off, enforced at intervention time.
        val toggleAt = System.currentTimeMillis()
        appDao.update(a.copy(hardLockEnabled = enabled, hardLockToggleAt = toggleAt))
    }

    fun setCategoryCap(categoryId: String, minutes: Int?) = viewModelScope.launch {
        val c = categoryDao.byId(categoryId) ?: return@launch
        categoryDao.upsert(c.copy(dailyMinutesCap = minutes))
    }
}

data class LimitsUiState(
    val apps: List<AppEntity>,
    val categories: List<CategoryEntity>,
) {
    companion object { val Empty = LimitsUiState(emptyList(), emptyList()) }
}
