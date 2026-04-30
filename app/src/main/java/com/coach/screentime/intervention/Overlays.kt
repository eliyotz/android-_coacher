package com.coach.screentime.intervention

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.ai.NegotiationViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

private val OverlayScrim = Color(0xCC0B1220)

@Composable
private fun OverlayBackdrop(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(OverlayScrim), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
internal fun MindfulnessPauseOverlay(
    appLabel: String,
    usedMinutes: Int,
    pauseSeconds: Int,
    onDismiss: () -> Unit,
) {
    OverlayBackdrop {
        var elapsed by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (elapsed < pauseSeconds) {
                delay(1000)
                elapsed += 1
            }
            onDismiss()
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(32.dp).widthIn(max = 360.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pause", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$appLabel — $usedMinutes min today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { (elapsed.toFloat() / pauseSeconds).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "${pauseSeconds - elapsed}s — take a breath. Is this what you meant to open?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss) { Text("Continue anyway") }
            }
        }
    }
}

@Composable
internal fun HardLockOverlay(
    appLabel: String,
    onSendHome: () -> Unit,
) {
    OverlayBackdrop {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(32.dp).widthIn(max = 360.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("$appLabel is locked for today", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You set a hard lock for $appLabel. It can't be lifted until tomorrow.\n\nIf you really want to disable hard lock, the toggle takes effect after a 24-hour cool-off — that's the point.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onSendHome,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("OK, take me home") }
            }
        }
    }
}

@Composable
internal fun NegotiationOverlay(
    packageName: String,
    appLabel: String,
    categoryName: String,
    usedMinutes: Int,
    triggerKind: String,
    interventionId: Long,
    onAccept: (extensionMinutes: Int) -> Unit,
    onReject: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            NegotiationOverlayEntryPoint::class.java,
        )
    }
    val vm = remember {
        NegotiationViewModel(
            geminiClient = entryPoint.geminiClient(),
            interventionDao = entryPoint.interventionDao(),
            settingsStore = entryPoint.settingsStore(),
            goalDao = entryPoint.goalDao(),
            rollupDao = entryPoint.rollupDao(),
            categoryDao = entryPoint.categoryDao(),
            appDao = entryPoint.appDao(),
        )
    }

    OverlayBackdrop {
        var reason by remember { mutableStateOf("") }
        var state by remember { mutableStateOf<NegotiationState>(NegotiationState.Asking) }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(32.dp).widthIn(max = 420.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                val triggerLabel = if (triggerKind == "category") "$categoryName limit reached" else "$appLabel limit reached"
                Text(triggerLabel, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$appLabel — $usedMinutes min today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                when (val s = state) {
                    NegotiationState.Asking -> {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Why do you need more time?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier.weight(1f),
                            ) { Text("Close app") }
                            Button(
                                onClick = {
                                    state = NegotiationState.Thinking
                                    vm.judge(
                                        packageName = packageName,
                                        appLabel = appLabel,
                                        categoryName = categoryName,
                                        usedMinutes = usedMinutes,
                                        triggerKind = triggerKind,
                                        reason = reason,
                                        interventionId = interventionId,
                                    ) { result -> state = NegotiationState.Decided(result) }
                                },
                                enabled = reason.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Submit") }
                        }
                    }
                    NegotiationState.Thinking -> {
                        Text("Coach is thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is NegotiationState.Decided -> {
                        val v = s.verdict
                        val accent = if (v.accept) Color(0xFF34D399) else Color(0xFFF87171)
                        Text(if (v.accept) "Granted" else "Denied", color = accent, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(v.explanation)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (v.accept) onAccept(v.extensionMinutes) else onReject()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                        ) { Text(if (v.accept) "Continue (${v.extensionMinutes} min)" else "Close app") }
                    }
                }
            }
        }
    }
}

internal sealed interface NegotiationState {
    data object Asking : NegotiationState
    data object Thinking : NegotiationState
    data class Decided(val verdict: NegotiationViewModel.Verdict) : NegotiationState
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
internal interface NegotiationOverlayEntryPoint {
    fun geminiClient(): com.coach.screentime.ai.GeminiClient
    fun interventionDao(): com.coach.screentime.data.db.dao.InterventionDao
    fun settingsStore(): com.coach.screentime.data.store.SettingsStore
    fun goalDao(): com.coach.screentime.data.db.dao.GoalDao
    fun rollupDao(): com.coach.screentime.data.db.dao.RollupDao
    fun categoryDao(): com.coach.screentime.data.db.dao.CategoryDao
    fun appDao(): com.coach.screentime.data.db.dao.AppDao
}
