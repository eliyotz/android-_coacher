package com.coach.screentime.enforcement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coach.screentime.intervention.OverlayManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives notification action taps from the task-check prompt.
 *  - YES: mark task working, schedule 30-min follow-up implicitly via worker.
 *  - NO:  show the reason-input overlay, then submit to the AI judge.
 */
@AndroidEntryPoint
class TaskCheckActionReceiver : BroadcastReceiver() {

    @Inject lateinit var engine: TaskEscalationEngine
    @Inject lateinit var overlayManager: OverlayManager

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_YES -> engine.markWorking(taskId)
                    ACTION_NO -> {
                        // Show the reason-input overlay; on submit it calls back into the engine.
                        overlayManager.showTaskCheck(taskId) { reason ->
                            scope.launch { engine.submitReason(taskId, reason) }
                        }
                    }
                    ACTION_REASON_SUBMITTED -> {
                        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
                        engine.submitReason(taskId, reason)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_YES = "com.coach.screentime.tasks.YES"
        const val ACTION_NO = "com.coach.screentime.tasks.NO"
        const val ACTION_REASON_SUBMITTED = "com.coach.screentime.tasks.REASON"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_REASON = "reason"
    }
}
