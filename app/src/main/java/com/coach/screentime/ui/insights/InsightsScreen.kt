package com.coach.screentime.ui.insights

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Last 7 days", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    SevenDayChart(state.sevenDayBars)
                }
            }
        }

        if (state.heatmap.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("When you use your phone", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Heatmap of minutes per hour, last 7 days. Darker = more.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        HourHeatmap(state.heatmap)
                    }
                }
            }
        }

        if (state.empty) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("No weekly report yet", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Reports run automatically every Sunday morning. Once you have at least a few days of data, you can also generate one now.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.runReportNow() }) { Text("Generate now") }
                    }
                }
            }
        } else {
            items(state.reports) { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Week of ${r.weekStart}", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(r.markdownBody, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item {
                Button(onClick = { viewModel.runReportNow() }) { Text("Re-run latest report") }
            }
        }
    }
}

@Composable
private fun HourHeatmap(grid: Array<IntArray>) {
    val max = (grid.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 1).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Hour-of-day axis: 0, 6, 12, 18 labels
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(20.dp))
            (0..23).forEach { h ->
                val show = h % 6 == 0
                Text(
                    if (show) h.toString() else " ",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        grid.indices.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val label = when (day) {
                    grid.size - 1 -> "Now"
                    grid.size - 2 -> "-1d"
                    else -> "-${grid.size - 1 - day}d"
                }
                Text(
                    label,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(20.dp),
                )
                (0..23).forEach { hour ->
                    val v = grid[day][hour]
                    val alpha = (v.toFloat() / max).coerceIn(0f, 1f)
                    val cellColor = if (v == 0) empty else primary.copy(alpha = 0.15f + 0.85f * alpha)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                            .padding(end = 1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cellColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun SevenDayChart(bars: List<DayBar>) {
    val max = (bars.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { b ->
            val pct = b.minutes.toFloat() / max
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(24.dp)
                        .height((100 * pct).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    b.date.takeLast(2),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
