package com.coach.screentime.reflection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.ui.theme.ScreenTimeCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReflectionActivity : ComponentActivity() {

    private val viewModel: ReflectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenTimeCoachTheme {
                val saved by viewModel.saved.collectAsState()
                if (saved) {
                    ThanksScreen(onDone = { finish() })
                } else {
                    ReflectionScreen(
                        onSave = { feeling, trigger ->
                            viewModel.save(feeling, trigger)
                        },
                        onSkip = { finish() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReflectionScreen(
    onSave: (feeling: Int, trigger: String) -> Unit,
    onSkip: () -> Unit,
) {
    var feeling by remember { mutableIntStateOf(3) }
    var trigger by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("How was yesterday?", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Quick reflection — feeds into your weekly coach report.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Text("Feeling", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            (1..5).forEach { value ->
                FeelingDot(
                    value = value,
                    selected = feeling == value,
                    onClick = { feeling = value },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("rough", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("great", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
        Text("What triggered any doomscrolling?", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "(optional, one or two words is fine)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = trigger,
            onValueChange = { trigger = it.take(280) },
            placeholder = { Text("e.g. couldn't sleep · bored at work · stressed") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSave(feeling, trigger) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Skip today") }
    }
}

@Composable
private fun FeelingDot(value: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(56.dp),
        onClick = onClick,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                value.toString(),
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ThanksScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Saved.", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "The coach will reference this on Sunday.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}
