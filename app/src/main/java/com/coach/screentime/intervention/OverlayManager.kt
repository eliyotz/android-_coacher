package com.coach.screentime.intervention

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds a Compose-based overlay to the system window. Caller passes a Composable;
 * we wire up a minimal lifecycle so Compose can run outside an Activity.
 *
 * One overlay at a time: showing a new one removes the previous.
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var current: ComposeOverlay? = null

    fun hasOverlayPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    fun overlayPermissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:${context.packageName}"),
    )

    @Synchronized
    fun showMindfulnessPause(
        packageName: String,
        appLabel: String,
        usedMinutes: Int,
        pauseSeconds: Int,
    ) {
        if (!hasOverlayPermission()) return
        show { onDone ->
            MindfulnessPauseOverlay(
                appLabel = appLabel,
                usedMinutes = usedMinutes,
                pauseSeconds = pauseSeconds,
                onContinue = { onDone() },
                onSendHome = {
                    onDone()
                    sendHome()
                },
            )
        }
    }

    @Synchronized
    fun showHardLock(packageName: String, appLabel: String) {
        if (!hasOverlayPermission()) return
        show { onDone ->
            HardLockOverlay(
                appLabel = appLabel,
                onSendHome = {
                    onDone()
                    sendHome()
                },
            )
        }
    }

    @Synchronized
    fun showNegotiation(
        packageName: String,
        appLabel: String,
        categoryName: String,
        usedMinutes: Int,
        triggerKind: String,
        interventionId: Long,
        onAccept: (extensionMinutes: Int) -> Unit,
    ) {
        if (!hasOverlayPermission()) return
        show { onDone ->
            NegotiationOverlay(
                packageName = packageName,
                appLabel = appLabel,
                categoryName = categoryName,
                usedMinutes = usedMinutes,
                triggerKind = triggerKind,
                interventionId = interventionId,
                onAccept = { ext ->
                    onAccept(ext)
                    onDone()
                },
                onReject = {
                    onDone()
                    sendHome()
                },
            )
        }
    }

    private fun sendHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(home)
    }

    @Synchronized
    fun showTaskCheck(
        taskId: String,
        onSubmit: (reason: String) -> Unit,
    ) {
        if (!hasOverlayPermission()) {
            // No overlay permission means we can't ask in-app; AI still escalates via the worker.
            return
        }
        show { onDone ->
            TaskCheckReasonOverlay(
                onSubmit = { reason ->
                    onSubmit(reason)
                    onDone()
                },
                onCancel = {
                    onDone()
                    sendHome()
                },
            )
        }
    }

    @Synchronized
    fun dismiss() {
        current?.let { runCatching { wm.removeView(it.view) } }
        current = null
    }

    private fun show(content: @Composable (onDone: () -> Unit) -> Unit) {
        dismiss()
        val overlay = ComposeOverlay.create(context) { onDone -> content(onDone) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        runCatching {
            wm.addView(overlay.view, params)
            overlay.attached()
            current = overlay
            overlay.onDoneCallback = { dismiss() }
        }
    }
}

/**
 * A ComposeView attached to the WindowManager needs its own lifecycle and
 * SavedStateRegistry owners. This wraps that bookkeeping.
 */
internal class ComposeOverlay private constructor(
    val view: ComposeView,
    private val lifecycleOwner: OverlayLifecycleOwner,
) {
    var onDoneCallback: (() -> Unit)? = null

    fun attached() {
        lifecycleOwner.start()
    }

    fun detached() {
        lifecycleOwner.stop()
    }

    companion object {
        fun create(
            context: Context,
            content: @Composable (onDone: () -> Unit) -> Unit,
        ): ComposeOverlay {
            val owner = OverlayLifecycleOwner()
            owner.created()
            val view = ComposeView(context)
            view.setViewTreeLifecycleOwner(owner)
            view.setViewTreeSavedStateRegistryOwner(owner)
            val overlay = ComposeOverlay(view, owner)
            view.setContent {
                content { overlay.onDoneCallback?.invoke() }
            }
            return overlay
        }
    }
}

internal class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val registry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun created() {
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun start() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
