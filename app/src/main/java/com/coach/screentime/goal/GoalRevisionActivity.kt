package com.coach.screentime.goal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.data.db.entities.GoalRevisionEntity
import com.coach.screentime.ui.theme.ScreenTimeCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GoalRevisionActivity : ComponentActivity() {

    private val viewModel: GoalRevisionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val revisionId = intent.getLongExtra(EXTRA_REVISION_ID, -1L)
        viewModel.load(revisionId)

        setContent {
            ScreenTimeCoachTheme {
                val state by viewModel.state.collectAsState()
                when (val s = state) {
                    GoalRevisionViewModel.UiState.Loading -> CenteredSpinner()
                    GoalRevisionViewModel.UiState.Empty -> EmptyScreen(onClose = { finish() })
                    GoalRevisionViewModel.UiState.Resolved -> CompletedScreen(onClose = { finish() })
                    is GoalRevisionViewModel.UiState.Loaded -> RevisionScreen(
                        revision = s.revision,
                        onAccept = { finalText -> viewModel.accept(s.revision.id, finalText) },
                        onDismiss = { viewModel.dismiss(s.revision.id) },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_REVISION_ID = "revision_id"
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No goal review pending.", fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun CompletedScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Saved.", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClose) { Text("Done") }
    }
}

@Composable
private fun RevisionScreen(
    revision: GoalRevisionEntity,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(revision.id) { mutableStateOf(revision.suggestedGoal) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scroll),
    ) {
        Text("Time to rethink your goal?", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Every four weekly reports the coach revisits whether your goal still fits what you're doing.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        if (revision.oldGoal.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your current goal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("\"${revision.oldGoal}\"")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Why the coach is suggesting a change",
                     fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(revision.rationale.ifBlank { "(no rationale)" })
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Suggested goal", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Edit before accepting if it doesn't sound quite right.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(200) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) { Text("Keep current goal") }
            Button(
                onClick = { onAccept(draft) },
                enabled = draft.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Save as my goal") }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Dismiss") }
    }
}
