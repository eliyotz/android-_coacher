package com.coach.screentime.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.coach.screentime.ui.components.Eyebrow
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.Card2
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Paper
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.RuleSoft
import com.coach.screentime.ui.theme.ScreenTimeCoachTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusActivity : ComponentActivity() {

    @Inject lateinit var focusManager: FocusManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenTimeCoachTheme {
                Box(Modifier.fillMaxSize().background(Paper)) {
                    FocusScreen(
                        onPick = { mins ->
                            lifecycleScope.launch {
                                if (mins == 0) focusManager.stop() else focusManager.start(mins)
                                finish()
                            }
                        },
                        onCancel = { finish() },
                        fetchActiveMinutes = {
                            val end = focusManager.activeUntil()
                            ((end - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L).toInt()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusScreen(
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
    fetchActiveMinutes: suspend () -> Int,
) {
    var activeMins by remember { mutableStateOf(0) }
    var customMins by remember { mutableStateOf("45") }
    LaunchedEffect(Unit) { activeMins = fetchActiveMinutes() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", style = CoachType.headlineMd, color = Ink, modifier = Modifier.padding(end = 14.dp))
            Eyebrow("Focus mode")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Hard-lock every flagged app.",
            style = CoachType.headlineMd.copy(fontSize = 30.sp),
            color = Ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "No mindfulness pause. No AI negotiation. Until the timer ends, the answer is no.",
            style = CoachType.bodyMd,
            color = Ink2,
        )

        if (activeMins > 0) {
            // Active state: show remaining card + end button
            Spacer(Modifier.height(24.dp))
            ActiveCard(activeMins)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onPick(0) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
            ) { Text("End focus now", style = CoachType.titleSm) }
        } else {
            // Picker state
            Spacer(Modifier.height(24.dp))
            FocusDurationRow(mins = 30, label = "Quick reset", sub = "Lunch break, walk, single deep task") { onPick(30) }
            Spacer(Modifier.height(10.dp))
            FocusDurationRow(mins = 60, label = "One hour", sub = "Default for most sessions") { onPick(60) }
            Spacer(Modifier.height(10.dp))
            FocusDurationRow(mins = 120, label = "Half a workday", sub = "2 hours") { onPick(120) }

            Spacer(Modifier.height(24.dp))
            Eyebrow("Custom", modifier = Modifier.padding(bottom = 8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Card)
                    .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = customMins,
                    onValueChange = { v -> customMins = v.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    textStyle = CoachType.mono.copy(fontSize = 22.sp),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTerracotta,
                        unfocusedBorderColor = Rule,
                    ),
                )
                Text("minutes", style = CoachType.meta, color = InkMute)
                Button(
                    onClick = { customMins.toIntOrNull()?.let { if (it > 0) onPick(it) } },
                    enabled = customMins.toIntOrNull()?.let { it > 0 } ?: false,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) { Text("Start", style = CoachType.titleSm, color = Card) }
            }

            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Card2)
                    .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Notifications, null, tint = InkMute, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Text(
                    "Persistent notification will show while focus is active. You can end it from the quick settings tile or this screen.",
                    style = CoachType.meta,
                    color = InkMute,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FocusDurationRow(mins: Int, label: String, sub: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Card),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RuleSoft),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 16.dp, 18.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Card2)
                    .border(1.dp, Rule, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(mins.toString(), style = CoachType.headlineSm.copy(fontSize = 22.sp), color = Ink)
                    Text("MIN", style = CoachType.eyebrow.copy(fontSize = 9.sp), color = InkMute)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = CoachType.titleSm, color = Ink)
                Text(sub, style = CoachType.meta, color = InkMute, modifier = Modifier.padding(top = 2.dp))
            }
            Text("›", style = CoachType.titleMd, color = InkMute)
        }
    }
}

@Composable
private fun ActiveCard(activeMins: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Ink)
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "${activeMins}m left",
                style = CoachType.headlineLg,
                color = com.coach.screentime.ui.theme.PaperOnDeep,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Focus mode is active. All flagged apps are locked.",
                style = CoachType.bodyMd,
                color = com.coach.screentime.ui.theme.PaperOnDeepMute,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
