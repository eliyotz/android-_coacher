package com.coach.screentime.focus

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.coach.screentime.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that cycles Focus mode: off → 30 min → 60 min → off.
 *
 * TileService can't use Hilt directly (it's not in the @AndroidEntryPoint
 * supported list yet), so we grab dependencies via EntryPointAccessors.
 */
@RequiresApi(Build.VERSION_CODES.N)
class FocusModeTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface FocusEntryPoint {
        fun focusManager(): FocusManager
    }

    private fun manager(): FocusManager =
        EntryPointAccessors.fromApplication(applicationContext, FocusEntryPoint::class.java)
            .focusManager()

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { refreshTile() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            manager().cycle()
            refreshTile()
        }
    }

    private suspend fun refreshTile() {
        val active = manager().isActive()
        val tile = qsTile ?: return
        if (active) {
            val until = manager().activeUntil()
            val mins = ((until - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L).toInt()
            tile.label = "Focus"
            tile.subtitle = "${mins}m left"
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.label = "Focus"
            tile.subtitle = "Tap to lock"
            tile.state = Tile.STATE_INACTIVE
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
        tile.updateTile()
    }
}
