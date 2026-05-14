package com.coach.screentime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.RuleSoft
import com.coach.screentime.ui.theme.SageGreen
import com.coach.screentime.ui.theme.WarnGold

@Composable
fun AdherenceRing(
    adherence: Float,
    label: String = "",
    sizeDp: Int = 148,
) {
    val color = when {
        adherence >= 0.8f -> SageGreen
        adherence >= 0.5f -> WarnGold
        else              -> AccentTerracotta
    }
    Box(modifier = Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = RuleSoft,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * adherence.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(adherence * 100).toInt()}",
                style = CoachType.headlineLg.copy(fontSize = 38.sp),
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "%",
                style = CoachType.eyebrow,
                color = InkMute,
                textAlign = TextAlign.Center,
            )
        }
    }
}
