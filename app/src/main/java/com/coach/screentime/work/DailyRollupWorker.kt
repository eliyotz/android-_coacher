package com.coach.screentime.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.util.Time
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/** Prunes sessions older than 90 days. Rollups are kept indefinitely (small). */
@HiltWorker
class DailyRollupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionDao: SessionDao,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val cutoff = LocalDate.now().minusDays(90).toString()
        sessionDao.pruneOlderThan(cutoff)
        return Result.success()
    }
}
