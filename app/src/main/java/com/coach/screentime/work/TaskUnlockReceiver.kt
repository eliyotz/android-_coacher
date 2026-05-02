package com.coach.screentime.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Re-runs the task escalation cycle on every device unlock so the user gets a
 * prompt as soon as they pick up the phone with overdue tasks waiting. The
 * heavy lifting still happens in [TaskCheckWorker]; this just kicks it off.
 *
 * Registered statically in the manifest with ACTION_USER_PRESENT.
 */
class TaskUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<TaskCheckWorker>().build()
        )
    }
}
