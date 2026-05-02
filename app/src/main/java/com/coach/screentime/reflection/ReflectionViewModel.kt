package com.coach.screentime.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.entities.ReflectionEntity
import com.coach.screentime.util.Time
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Stores a 1–5 feeling + free-text trigger for *yesterday* — the morning reflection
 * is reflecting on the previous day.
 */
@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val reflectionDao: ReflectionDao,
) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(feeling: Int, triggerText: String) {
        viewModelScope.launch {
            val yesterday = LocalDate.now().minusDays(1).toString()
            reflectionDao.upsert(
                ReflectionEntity(
                    dateLocal = yesterday,
                    feeling = feeling.coerceIn(1, 5),
                    triggerText = triggerText.trim(),
                    createdAt = System.currentTimeMillis(),
                )
            )
            _saved.value = true
        }
    }
}
