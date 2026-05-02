package com.coach.screentime.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.ui.components.AdherenceRing
import com.coach.screentime.ui.components.PunishmentBanner
import com.coach.screentime.ui.components.UsageRow

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.activePunishment?.let { p ->
            item { PunishmentBanner(p) }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AdherenceRing(adherence = state.adherence, label = "healthy\nopens")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${formatMin(state.totalMinutes)} · ${state.totalOpens} opens today",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.streak > 0 || state.worstHourLabel != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.streak > 0) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("🔥 ${state.streak} day streak") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0x33F59E0B),
                                    ),
                                )
                            }
                            state.worstHourLabel?.let { label ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Worst hour: $label") },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.categories.any { it.minutes > 0 || it.capMinutes != null }) {
            item {
                Text("Categories", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            items(state.categories.filter { it.minutes > 0 || it.capMinutes != null }) { c ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        UsageRow(label = c.name, minutes = c.minutes, opens = c.opens, capMinutes = c.capMinutes)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("Apps", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        items(state.apps) { a ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    UsageRow(label = a.label, minutes = a.minutes, opens = a.opens, capMinutes = a.capMinutes)
                    a.deltaVsWeekAvgPct?.let { delta ->
                        if (delta != 0) {
                            val arrow = if (delta > 0) "↑" else "↓"
                            val color = if (delta > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                            Text(
                                text = "$arrow ${kotlin.math.abs(delta)}% vs avg last 7 d",
                                fontSize = 11.sp,
                                color = color,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMin(min: Int): String {
    if (min < 60) return "${min}m"
    val h = min / 60
    val m = min % 60
    return "${h}h ${m}m"
}
