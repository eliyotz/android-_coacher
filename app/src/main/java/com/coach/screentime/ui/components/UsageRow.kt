package com.coach.screentime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.SageGreen

@Composable
fun UsageRow(
    label: String,
    minutes: Int,
    opens: Int,
    capMinutes: Int?,
    packageName: String = "",
    deltaVsWeekAvgPct: Int? = null,
    hardLock: Boolean = false,
) {
    val over = capMinutes != null && minutes > capMinutes
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (packageName.isNotBlank()) {
                AppGlyph(displayName = label, brandColor = brandColorForApp(label), size = 34)
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = CoachType.titleSm, color = Ink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = formatMin(minutes),
                        style = CoachType.mono,
                        color = if (over) AccentTerracotta else Ink2,
                    )
                    if (capMinutes != null) {
                        Text("· cap ${formatMin(capMinutes)}", style = CoachType.meta, color = InkMute)
                    }
                    Text("· $opens opens", style = CoachType.meta, color = InkMute)
                }
            }
            deltaVsWeekAvgPct?.let { delta ->
                if (delta != 0) {
                    val arrow = if (delta > 0) "↑" else "↓"
                    val col = if (delta > 0) AccentTerracotta else SageGreen
                    Text(
                        "$arrow${kotlin.math.abs(delta)}%",
                        style = CoachType.mono.copy(fontSize = 11.sp),
                        color = col,
                    )
                }
            }
        }
        if (capMinutes != null) {
            Spacer(Modifier.height(8.dp))
            UsageBar(used = minutes, cap = capMinutes, hardLock = hardLock)
        }
    }
}

internal fun formatMin(min: Int): String {
    if (min < 60) return "${min}m"
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}
