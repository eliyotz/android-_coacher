package com.coach.screentime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shows a ring representing the user's adherence (0..1) with a label inside.
 * Adherence here is a proxy: % of today's sessions under [healthySessionMaxSec].
 */
@Composable
fun AdherenceRing(
    adherence: Float,
    label: String,
    sizeDp: Int = 160,
) {
    val color = when {
        adherence >= 0.8f -> Color(0xFF34D399)
        adherence >= 0.5f -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val stroke = 14.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * adherence.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
        }
        Text(
            text = "${(adherence * 100).toInt()}%\n$label",
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
