package com.coach.screentime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.coach.screentime.data.db.entities.PunishmentEntity
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.InkDeep2
import com.coach.screentime.ui.theme.PaperOnDeep
import com.coach.screentime.ui.theme.PaperOnDeepMute

@Composable
fun PunishmentBanner(p: PunishmentEntity) {
    val remainMs = (p.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    val hours = (remainMs / (60 * 60_000L)).toInt()
    val mins  = ((remainMs % (60 * 60_000L)) / 60_000L).toInt()
    val timeLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InkDeep2)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = AccentTerracotta,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow("Coach intervention", color = AccentTerracotta)
            Text(
                p.rationale.ifBlank { "Blocked for $timeLabel." },
                style = CoachType.titleSm,
                color = PaperOnDeep,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                "Expires in $timeLabel",
                style = CoachType.bodySm,
                color = PaperOnDeepMute,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
