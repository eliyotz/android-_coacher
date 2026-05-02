package com.coach.screentime.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coach.screentime.auth.GoogleAuthRepository
import com.coach.screentime.enforcement.TaskEscalationEngine
import com.coach.screentime.tasks.TasksRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Pulls Google Tasks, mirrors them, and runs the escalation cycle.
 * Skipped if the user hasn't connected Google Tasks.
 */
@HiltWorker
class TaskCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepo: GoogleAuthRepository,
    private val tasksRepo: TasksRepository,
    private val engine: TaskEscalationEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!authRepo.isOAuthConfigured() || !authRepo.isConnected) return Result.success()
        val syncResult = tasksRepo.sync()
        if (syncResult.isFailure) return Result.retry()
        engine.runCycle()
        return Result.success()
    }
}
