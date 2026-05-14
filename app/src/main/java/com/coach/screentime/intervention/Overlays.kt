package com.coach.screentime.intervention

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.ai.NegotiationViewModel
import com.coach.screentime.ui.components.CoachAvatar
import com.coach.screentime.ui.components.Eyebrow
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Alert
import com.coach.screentime.ui.theme.AlertSoft
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.Card2
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkDeep
import com.coach.screentime.ui.theme.InkDeep2
import com.coach.screentime.ui.theme.InkDeep3
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.MindfulSage
import com.coach.screentime.ui.theme.MindfulSurface
import com.coach.screentime.ui.theme.PaperOnDeep
import com.coach.screentime.ui.theme.PaperOnDeepMute
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.SageGreen
import dagger.hilt.android.EntryPointAccessors

// ─────────────────────────────────────────────────────────────────────────────
// Mindfulness pause – full-bleed breathing screen (cool green-black)
// ─────────────────────────────────────────────────────────────────────────────

private val mindfulPrompts = listOf(
    "Breathe in, then ask: what am I actually looking for here?",
    "Notice the impulse. You don't have to act on it.",
    "Three slow breaths first. Then decide.",
    "What were you doing before you picked up the phone?",
    "Is this what you want to be doing right now?",
)

@Composable
internal fun MindfulnessPauseOverlay(
    appLabel: String,
    usedMinutes: Int,
    pauseSeconds: Int,
    onContinue: () -> Unit,
    onSendHome: () -> Unit,
) {
    val hour = java.time.LocalTime.now().hour
    val prompt = mindfulPrompts[hour % mindfulPrompts.size]
    var isHolding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val remainingFraction = 1f - progress.value
            val remainingMs = (remainingFraction * pauseSeconds * 1000).toInt().coerceAtLeast(50)
            progress.animateTo(1f, animationSpec = tween(durationMillis = remainingMs, easing = LinearEasing))
            if (progress.value >= 1f) onContinue()
        } else {
            progress.animateTo(0f, animationSpec = tween(durationMillis = 250))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MindfulSurface),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Eyebrow("Pause", color = MindfulSage)
                Text(
                    "opening · $appLabel",
                    style = CoachType.mono.copy(fontSize = 11.sp),
                    color = MindfulSage,
                )
            }

            // Center content
            Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Concentric breathing rings + counter
                    Box(
                        Modifier.size(260.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        listOf(1f, 0.78f, 0.56f, 0.34f).forEachIndexed { i, s ->
                            val ringAlpha = 0.15f + (1f - s) * 0.25f
                            Box(
                                Modifier
                                    .size((260 * s).dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MindfulSage.copy(alpha = ringAlpha), CircleShape),
                            )
                        }
                        // Center circle
                        Box(
                            Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D3A33))
                                .border(1.dp, MindfulSage.copy(alpha = 0.45f), CircleShape)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        isHolding = true
                                        waitForUpOrCancellation()
                                        isHolding = false
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val secondsLeft = (pauseSeconds * (1f - progress.value)).toInt().coerceAtLeast(0)
                            Text(
                                secondsLeft.toString(),
                                style = CoachType.display.copy(fontSize = 52.sp),
                                color = Color(0xFFE6EFE0),
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))
                    Text(
                        prompt,
                        style = CoachType.headlineSm.copy(fontStyle = FontStyle.Italic),
                        color = PaperOnDeep,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "$usedMinutes opens today",
                        style = CoachType.meta,
                        color = MindfulSage,
                    )
                }
            }

            // Progress bar + footer
            Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MindfulSage.copy(alpha = 0.2f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.value)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MindfulSage),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("auto-continues", style = CoachType.mono.copy(fontSize = 11.sp), color = MindfulSage)
                    Text("tap & hold to skip", style = CoachType.mono.copy(fontSize = 11.sp), color = MindfulSage)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hard lock – full-screen dark, Ulysses contract
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun HardLockOverlay(
    appLabel: String,
    hardLockToggleAt: Long = 0L,
    streak: Int = 0,
    onSendHome: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(InkDeep)
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Hard lock · $appLabel", color = AccentTerracotta)
                Icon(Icons.Filled.Lock, null, tint = AccentTerracotta, modifier = Modifier.size(18.dp))
            }

            // Central copy
            Spacer(Modifier.weight(1f))
            Text(
                "You signed a contract with yourself.",
                style = CoachType.headlineLg,
                color = PaperOnDeep,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            if (hardLockToggleAt > 0L) {
                val dt = java.time.Instant.ofEpochMilli(hardLockToggleAt)
                    .atZone(java.time.ZoneId.systemDefault())
                val dateStr = "${dt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}, ${dt.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())} ${dt.dayOfMonth} · ${dt.hour.toString().padStart(2,'0')}:${dt.minute.toString().padStart(2,'0')}"
                Text(
                    "On $dateStr you enabled hard-lock on $appLabel.\n\nNo negotiation. No extension. The lock disables 24 hours after you toggle it off — and the toggle's in Limits.",
                    style = CoachType.bodyMd,
                    color = PaperOnDeepMute,
                )
            } else {
                Text(
                    "$appLabel is hard-locked. No negotiation. No extension.\n\nThe lock disables 24 hours after you toggle it off — the toggle is in Limits.",
                    style = CoachType.bodyMd,
                    color = PaperOnDeepMute,
                )
            }
            Spacer(Modifier.weight(1f))

            // Footer info card
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(InkDeep2)
                    .border(1.dp, InkDeep3, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Lock ends", color = PaperOnDeepMute)
                    Text(
                        "tomorrow · 00:00",
                        style = CoachType.mono.copy(fontSize = 22.sp),
                        color = PaperOnDeep,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (streak > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Eyebrow("Streak protected", color = PaperOnDeepMute)
                        Text(
                            "$streak days",
                            style = CoachType.headlineSm,
                            color = PaperOnDeep,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onSendHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
            ) { Text("Take me home", style = CoachType.titleSm, color = Card) }

            Spacer(Modifier.height(10.dp))
            Text(
                "Pressing back also goes home. Same destination.",
                style = CoachType.meta,
                color = PaperOnDeepMute,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AI Negotiation – bottom sheet over dimmed app
// ─────────────────────────────────────────────────────────────────────────────

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
            geminiClient     = entryPoint.geminiClient(),
            interventionDao  = entryPoint.interventionDao(),
            settingsStore    = entryPoint.settingsStore(),
            goalDao          = entryPoint.goalDao(),
            rollupDao        = entryPoint.rollupDao(),
            categoryDao      = entryPoint.categoryDao(),
            appDao           = entryPoint.appDao(),
        )
    }

    var reason by remember { mutableStateOf("") }
    var state  by remember { mutableStateOf<NegotiationState>(NegotiationState.Asking) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF14130F).copy(alpha = 0.62f)),
    ) {
        // App indicator pill at the top
        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF14130F).copy(alpha = 0.72f))
                    .border(1.dp, PaperOnDeep.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = AccentTerracotta, modifier = Modifier.size(12.dp))
                Text(
                    "$appLabel · over ${usedMinutes}m cap",
                    style = CoachType.bodySm,
                    color = PaperOnDeep,
                )
            }
        }

        // Bottom sheet
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Card,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                // Drag handle
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Rule)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(14.dp))

                // Coach header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoachAvatar(size = 28)
                    Column {
                        Text("The coach is listening.", style = CoachType.titleSm, color = Ink)
                        Text("Tell it why you need more time.", style = CoachType.meta, color = InkMute)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Verdict block (only when decided)
                when (val s = state) {
                    is NegotiationState.Decided -> {
                        val v = s.verdict
                        if (v.accept) {
                            AcceptBlock(v)
                        } else {
                            RejectBlock(v)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    else -> Unit
                }

                // Input field
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card2)
                        .border(1.dp, Rule, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Column {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { if (it.length <= 280) reason = it },
                            textStyle = CoachType.bodyMd.copy(fontSize = 14.5.sp),
                            placeholder = { Text("Why do you need more time?", style = CoachType.bodyMd, color = InkMute) },
                            minLines = 3,
                            maxLines = 5,
                            enabled = state is NegotiationState.Asking,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${reason.length} / 280", style = CoachType.meta, color = InkMute)
                            when (state) {
                                NegotiationState.Thinking -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                                        shape = RoundedCornerShape(10.dp),
                                    ) { Text("Thinking…", style = CoachType.bodySm, color = Card) }
                                }
                                is NegotiationState.Decided -> {
                                    val v = (state as NegotiationState.Decided).verdict
                                    Button(
                                        onClick = { if (v.accept) onAccept(v.extensionMinutes) else onReject() },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (v.accept) SageGreen else AccentTerracotta),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text(
                                            if (v.accept) "Continue (${v.extensionMinutes}m)" else "Close app",
                                            style = CoachType.bodySm,
                                            color = Card,
                                        )
                                    }
                                }
                                else -> {
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
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                                        shape = RoundedCornerShape(10.dp),
                                    ) { Text("Send to coach", style = CoachType.bodySm, color = Card) }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔔", style = CoachType.meta)
                    Text(
                        "Coach has memory. It's seen your reasons today.",
                        style = CoachType.meta,
                        color = InkMute,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AcceptBlock(v: NegotiationViewModel.Verdict) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE5ECDB))
            .border(1.dp, Color(0xFFC7D4B5), RoundedCornerShape(12.dp))
            .padding(12.dp, 12.dp, 12.dp, 12.dp),
    ) {
        Eyebrow("Verdict · ${v.extensionMinutes} more minutes", color = SageGreen, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "\"${v.explanation}\"",
            style = CoachType.headlineSm.copy(fontStyle = FontStyle.Italic, fontSize = 16.sp),
            color = Ink,
        )
    }
}

@Composable
private fun RejectBlock(v: NegotiationViewModel.Verdict) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AlertSoft.copy(alpha = 0.33f))
            .border(1.dp, AlertSoft, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Eyebrow("Verdict · denied", color = Alert, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "\"${v.explanation}\"",
            style = CoachType.headlineSm.copy(fontStyle = FontStyle.Italic, fontSize = 16.sp),
            color = Ink,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task check reason overlay (unchanged logic, updated styling)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun TaskCheckReasonOverlay(
    onSubmit: (reason: String) -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC14130F)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Card,
        ) {
            var reason by remember { mutableStateOf("") }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                CoachAvatar(size = 28)
                Spacer(Modifier.height(12.dp))
                Text("Why aren't you doing it?", style = CoachType.headlineSm, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The coach will decide whether to grant a delay or take action. Be honest — repeated dismissals stack up.",
                    style = CoachType.bodyMd,
                    color = Ink2,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(500) },
                    label = { Text("Your reason", style = CoachType.meta) },
                    textStyle = CoachType.bodyMd,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTerracotta,
                        unfocusedBorderColor = Rule,
                        focusedContainerColor = Card2,
                        unfocusedContainerColor = Card2,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                        colors = androidx.compose.material3.OutlinedButtonDefaults.outlinedButtonColors(contentColor = Ink),
                    ) { Text("Cancel", style = CoachType.titleSm) }
                    Button(
                        onClick = { onSubmit(reason.trim()) },
                        enabled = reason.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                    ) { Text("Send to coach", style = CoachType.titleSm, color = Card) }
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
