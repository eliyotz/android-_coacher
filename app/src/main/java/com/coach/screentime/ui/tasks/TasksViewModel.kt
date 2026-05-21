package com.coach.screentime.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.auth.GoogleAuthRepository
import com.coach.screentime.data.db.dao.PunishmentDao
import com.coach.screentime.data.db.dao.TaskDao
import com.coach.screentime.data.db.dao.TaskDelayDao
import com.coach.screentime.data.db.dao.TaskGrantCount
import com.coach.screentime.data.db.dao.TaskStateDao
import com.coach.screentime.data.db.entities.PunishmentEntity
import com.coach.screentime.data.db.entities.TaskEntity
import com.coach.screentime.data.db.entities.TaskStateEntity
import com.coach.screentime.enforcement.TaskEscalationEngine
import com.coach.screentime.tasks.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val authRepo: GoogleAuthRepository,
    private val taskDao: TaskDao,
    private val taskStateDao: TaskStateDao,
    private val taskDelayDao: TaskDelayDao,
    private val punishmentDao: PunishmentDao,
    private val tasksRepo: TasksRepository,
    private val engine: TaskEscalationEngine,
) : ViewModel() {

    private val _connectionTick = MutableStateFlow(0)
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** The task whose action sheet is currently open, or null if closed. */
    private val _sheetTask = MutableStateFlow<TaskRow?>(null)
    val sheetTask: StateFlow<TaskRow?> = _sheetTask.asStateFlow()

    /** Coach reply to show as a Snackbar. Cleared by [clearCoachReply]. */
    private val _coachReply = MutableStateFlow<String?>(null)
    val coachReply: StateFlow<String?> = _coachReply.asStateFlow()

    // 5-flow combine: replaces N×2 per-task DB calls with 2 batch reactive queries.
    val state: StateFlow<TasksUiState> = combine(
        taskDao.observeOpen(),
        taskStateDao.observeAll(),
        taskDelayDao.observeGrantedCounts(),
        punishmentDao.observeActive(System.currentTimeMillis()),
        _connectionTick,
    ) { tasks, allStates, grantCounts, punishments, _ ->
        if (!authRepo.isOAuthConfigured()) return@combine TasksUiState.NotConfigured
        if (!authRepo.isConnected) return@combine TasksUiState.Disconnected

        val stateMap = allStates.associateBy { it.googleId }
        val countMap = grantCounts.associate { it.googleId to it.grantCount }
        val now = System.currentTimeMillis()
        val rows = tasks.map { t ->
            val st = stateMap[t.googleId]
            TaskRow(
                task = t,
                state = st?.state ?: "untouched",
                delayedUntilMs = st?.delayedUntilMs ?: 0L,
                grantedDelayCount = countMap[t.googleId] ?: 0,
                isOverdue = (t.dueDateMs ?: Long.MAX_VALUE) < now,
            )
        }.sortedWith(
            compareByDescending<TaskRow> { it.isOverdue }
                .thenBy { it.task.dueDateMs ?: Long.MAX_VALUE },
        )
        TasksUiState.Ready(
            email = authRepo.accountEmail,
            tasks = rows,
            activePunishments = punishments,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState.Loading)

    fun openSheet(row: TaskRow) { _sheetTask.value = row }
    fun closeSheet() { _sheetTask.value = null }

    fun markWorking(googleId: String) {
        closeSheet()
        viewModelScope.launch { engine.markWorking(googleId) }
    }

    fun markCompleted(googleId: String) {
        closeSheet()
        viewModelScope.launch { engine.markCompleted(googleId) }
    }

    fun submitReason(googleId: String, reason: String) {
        closeSheet()
        viewModelScope.launch {
            val reply = engine.submitReason(googleId, reason)
            if (reply != null) _coachReply.value = reply
        }
    }

    fun clearCoachReply() { _coachReply.value = null }

    fun signalConnectionChange() {
        _connectionTick.value = _connectionTick.value + 1
    }

    fun sync() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            runCatching { tasksRepo.sync() }
            _syncing.value = false
            signalConnectionChange()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            tasksRepo.signOut()
            tasksRepo.clearMirror()
            signalConnectionChange()
        }
    }
}

sealed interface TasksUiState {
    data object Loading : TasksUiState
    data object NotConfigured : TasksUiState
    data object Disconnected : TasksUiState
    data class Ready(
        val email: String?,
        val tasks: List<TaskRow>,
        val activePunishments: List<PunishmentEntity>,
    ) : TasksUiState
}

data class TaskRow(
    val task: TaskEntity,
    val state: String,
    val delayedUntilMs: Long,
    val grantedDelayCount: Int,
    val isOverdue: Boolean,
)
