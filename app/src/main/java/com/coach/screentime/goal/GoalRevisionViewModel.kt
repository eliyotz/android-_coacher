package com.coach.screentime.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.GoalRevisionDao
import com.coach.screentime.data.db.entities.GoalEntity
import com.coach.screentime.data.db.entities.GoalRevisionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalRevisionViewModel @Inject constructor(
    private val goalRevisionDao: GoalRevisionDao,
    private val goalDao: GoalDao,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(revisionId: Long) {
        viewModelScope.launch {
            // If a specific id was passed, prefer it; otherwise grab the newest pending.
            val rev = if (revisionId > 0) goalRevisionDao.byId(revisionId) else goalRevisionDao.pending()
            _state.value = if (rev == null) UiState.Empty else UiState.Loaded(rev)
        }
    }

    fun accept(revisionId: Long, finalText: String) = viewModelScope.launch {
        val rev = goalRevisionDao.byId(revisionId) ?: return@launch
        goalDao.deactivateAll()
        if (finalText.isNotBlank()) {
            goalDao.insert(
                GoalEntity(
                    text = finalText.trim(),
                    active = true,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
        val edited = finalText.trim() != rev.suggestedGoal.trim()
        goalRevisionDao.update(
            rev.copy(
                resolution = if (edited) "edited" else "accepted",
                resolvedGoal = finalText.trim(),
                resolvedAt = System.currentTimeMillis(),
            )
        )
        _state.value = UiState.Resolved
    }

    fun dismiss(revisionId: Long) = viewModelScope.launch {
        val rev = goalRevisionDao.byId(revisionId) ?: return@launch
        goalRevisionDao.update(
            rev.copy(
                resolution = "dismissed",
                resolvedGoal = null,
                resolvedAt = System.currentTimeMillis(),
            )
        )
        _state.value = UiState.Resolved
    }

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data object Resolved : UiState
        data class Loaded(val revision: GoalRevisionEntity) : UiState
    }
}
