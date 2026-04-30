package com.coach.screentime.tracking

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.coach.screentime.R
import com.coach.screentime.intervention.InterventionEngine
import com.coach.screentime.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundAppService : LifecycleService() {

    @Inject lateinit var poller: UsageStatsPoller
    @Inject lateinit var currentAppTracker: CurrentAppTracker
    @Inject lateinit var sessionAggregator: SessionAggregator
    @Inject lateinit var interventionEngine: InterventionEngine

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> lifecycleScope.launch {
                    sessionAggregator.onScreenOff(System.currentTimeMillis())
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        poller.start()
        observeForegroundAppChanges()
        interventionEngine.start(lifecycleScope)
    }

    private fun observeForegroundAppChanges() {
        lifecycleScope.launch {
            currentAppTracker.state
                .distinctUntilChangedBy { it.packageName }
                .collect { state ->
                    if (state.packageName.isNotBlank()) {
                        sessionAggregator.onForegroundAppChanged(state.packageName, state.ts)
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        poller.stop()
        super.onDestroy()
    }

    private fun startInForeground() {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif: Notification = NotificationCompat.Builder(this, NotifChannels.TRACKER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_tracker_title))
            .setContentText(getString(R.string.notif_tracker_text))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotifChannels.TRACKER_NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NotifChannels.TRACKER_NOTIF_ID, notif)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ForegroundAppService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ForegroundAppService::class.java))
        }
    }
}
