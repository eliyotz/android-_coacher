package com.coach.screentime.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.auth.GoogleAuthRepository
import com.coach.screentime.data.db.dao.PunishmentDao
import com.coach.screentime.data.db.dao.TaskDao
import com.coach.screentime.data.db.dao.TaskDelayDao
import com.coach.screentime.data.db.dao.TaskStateDao
import com.coach.screentime.data.db.entities.PunishmentEntity
import com.coach.screentime.data.db.entities.TaskDelayEntity
import com.coach.screentime.data.db.entities.TaskEntity
import com.coach.screentime.data.db.entities.TaskStateEntity
import com.coach.screentime.tasks.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
) : ViewModel() {

    private val _connectionTick = MutableStateFlow(0)

    val state: StateFlow<TasksUiState> = combine(
        taskDao.observeOpen(),
        punishmentDao.observeActive(System.currentTimeMillis()),
        _connectionTick,
    ) { tasks, punishments, _ ->
        if (!authRepo.isOAuthConfigured()) return@combine TasksUiState.NotConfigured
        if (!authRepo.isConnected) return@combine TasksUiState.Disconnected

        val now = System.currentTimeMillis()
        val rows = tasks.map { t ->
            val st = taskStateDao.byId(t.googleId)
            val delays = taskDelayDao.grantedCountForTask(t.googleId)
            TaskRow(
                task = t,
                state = st?.state ?: "untouched",
                delayedUntilMs = st?.delayedUntilMs ?: 0L,
                grantedDelayCount = delays,
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

    fun signalConnectionChange() {
        _connectionTick.value = _connectionTick.value + 1
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
