package com.coach.screentime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UsageRow(
    label: String,
    minutes: Int,
    opens: Int,
    capMinutes: Int?,
) {
    val pct = if (capMinutes != null && capMinutes > 0) (minutes.toFloat() / capMinutes).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${formatMin(minutes)} · $opens opens",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (capMinutes != null) {
            Spacer(Modifier.padding(2.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatMin(min: Int): String {
    if (min < 60) return "${min}m"
    val h = min / 60
    val m = min % 60
    return "${h}h ${m}m"
}
