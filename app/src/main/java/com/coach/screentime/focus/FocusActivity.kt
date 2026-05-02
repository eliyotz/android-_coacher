package com.coach.screentime.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.coach.screentime.ui.theme.ScreenTimeCoachTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Landing for the home-screen shortcut and the in-app Focus button.
 * Lets the user pick a duration (30 / 60 min, or end) and finishes.
 */
@AndroidEntryPoint
class FocusActivity : ComponentActivity() {

    @Inject lateinit var focusManager: FocusManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenTimeCoachTheme {
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

@Composable
private fun FocusScreen(
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
    fetchActiveMinutes: suspend () -> Int,
) {
    var activeMins by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { activeMins = fetchActiveMinutes() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Focus mode", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Hard-locks every flagged app. No negotiation.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (activeMins > 0) {
            Spacer(Modifier.height(8.dp))
            Text("Currently active: ${activeMins}m left", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onPick(30) }, modifier = Modifier.fillMaxWidth()) { Text("30 minutes") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onPick(60) }, modifier = Modifier.fillMaxWidth()) { Text("1 hour") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onPick(120) }, modifier = Modifier.fillMaxWidth()) { Text("2 hours") }
        if (activeMins > 0) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { onPick(0) }, modifier = Modifier.fillMaxWidth()) { Text("End focus now") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
