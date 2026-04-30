package com.coach.screentime.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coach.screentime.data.store.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsStore: SettingsStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (settingsStore.snapshot().onboarded) {
                    ForegroundAppService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
