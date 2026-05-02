package com.coach.screentime.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives broadcasts from notification actions and Quick Settings tile clicks.
 * Lightweight wrapper around [FocusManager].
 */
@AndroidEntryPoint
class FocusActionReceiver : BroadcastReceiver() {

    @Inject lateinit var focusManager: FocusManager

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_START -> {
                        val minutes = intent.getIntExtra(EXTRA_MINUTES, 30)
                        focusManager.start(minutes)
                    }
                    ACTION_STOP -> focusManager.stop()
                    ACTION_CYCLE -> focusManager.cycle()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.coach.screentime.focus.START"
        const val ACTION_STOP = "com.coach.screentime.focus.STOP"
        const val ACTION_CYCLE = "com.coach.screentime.focus.CYCLE"
        const val EXTRA_MINUTES = "minutes"
    }
}
