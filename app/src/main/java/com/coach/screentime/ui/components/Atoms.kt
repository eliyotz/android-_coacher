package com.coach.screentime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Alert
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.Card2
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Paper
import com.coach.screentime.ui.theme.Paper2
import com.coach.screentime.ui.theme.RuleSoft
import com.coach.screentime.ui.theme.SageGreen

@Composable
fun Eyebrow(text: String, color: Color = InkMute, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = CoachType.eyebrow,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun CoachAvatar(size: Int = 22) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Text("◉", color = Paper, fontSize = (size * 0.5f).sp)
    }
}

@Composable
fun Pill(
    text: String,
    leading: (@Composable () -> Unit)? = null,
    background: Color = Card,
    border: Color = RuleSoft,
    textColor: Color = Ink2,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(text, style = CoachType.bodySm.copy(fontWeight = FontWeight.Medium), color = textColor)
    }
}

@Composable
fun AppGlyph(displayName: String, brandColor: Color, size: Int = 34) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(brandColor),
        contentAlignment = Alignment.Center,
    ) {
        val initials = displayName
            .split(' ', limit = 2)
            .joinToString("") { it.firstOrNull()?.toString() ?: "" }
            .take(2)
            .ifBlank { displayName.take(2) }
        Text(
            initials,
            style = CoachType.headlineSm.copy(fontSize = (size * 0.46f).sp),
            color = Card,
        )
    }
}

@Composable
fun UsageBar(used: Int, cap: Int, hardLock: Boolean = false, modifier: Modifier = Modifier) {
    val pct = (used.toFloat() / cap.coerceAtLeast(1)).coerceIn(0f, 1f)
    val fillColor = when {
        hardLock   -> Alert
        used > cap -> AccentTerracotta
        else       -> SageGreen
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(RuleSoft),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(pct)
                .clip(RoundedCornerShape(4.dp))
                .background(fillColor),
        )
    }
}

@Composable
fun SegmentedTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Paper2)
            .border(1.dp, RuleSoft, RoundedCornerShape(999.dp))
            .padding(3.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Ink else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    label,
                    style = CoachType.bodySm.copy(fontWeight = FontWeight.Medium),
                    color = if (active) Paper else Ink2,
                )
            }
        }
    }
}

fun brandColorForApp(displayName: String): Color {
    val name = displayName.lowercase()
    return when {
        name.contains("instagram") -> Color(0xFFC25A33)
        name.contains("tiktok")    -> Color(0xFF1A1916)
        name.contains("twitter") || name.contains(" x ") || name == "x" -> Color(0xFF3D3A33)
        name.contains("reddit")    -> Color(0xFFA24923)
        name.contains("youtube")   -> Color(0xFFA52E1A)
        name.contains("slack")     -> Color(0xFF6E7F5E)
        name.contains("gmail")     -> Color(0xFFC58A2E)
        name.contains("chrome")    -> Color(0xFF5E6B7A)
        name.contains("whatsapp")  -> Color(0xFF6E7F5E)
        name.contains("maps")      -> Color(0xFF7A7466)
        name.contains("linkedin")  -> Color(0xFF3D3A33)
        name.contains("facebook")  -> Color(0xFF3D3A33)
        name.contains("snapchat")  -> Color(0xFFC58A2E)
        name.contains("netflix")   -> Color(0xFFA52E1A)
        name.contains("discord")   -> Color(0xFF5E6B7A)
        else                       -> Color(0xFF7A7466)
    }
}
