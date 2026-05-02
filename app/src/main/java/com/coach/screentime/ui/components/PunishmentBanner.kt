package com.coach.screentime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.data.db.entities.PunishmentEntity

@Composable
fun PunishmentBanner(p: PunishmentEntity) {
    val mins = ((p.expiresAt - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L).toInt()
    val accent = when (p.severity) {
        "harsh" -> Color(0xFFB91C1C)
        "medium" -> Color(0xFFD97706)
        else -> Color(0xFFF59E0B)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Coach lockdown active",
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                p.rationale.ifBlank { "Punishment in effect — no negotiation." },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Expires in ${mins / 60}h ${mins % 60}m",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}
