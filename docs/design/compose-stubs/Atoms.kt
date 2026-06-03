// app/src/main/java/com/coach/screentime/ui/components/Atoms.kt
//
// Small atoms used across every screen. Built in Compose with our token palette.
// Drop alongside the existing AdherenceRing / UsageRow / PunishmentBanner files.

package com.coach.screentime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.coach.screentime.ui.theme.*

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
        // Substitute with painterResource(R.drawable.ic_eye) tinted to Paper.
        Text("◉", color = Paper)
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

/**
 * 6dp horizontal track with a colored fill.
 * Sage = under, terracotta = over, alert = hard-lock.
 */
@Composable
fun UsageBar(used: Int, cap: Int, hardLock: Boolean = false, modifier: Modifier = Modifier) {
    val pct = (used.toFloat() / cap.coerceAtLeast(1)).coerceIn(0f, 1f)
    val fillColor = when {
        hardLock           -> Alert
        used > cap         -> AccentTerracotta
        else               -> SageGreen
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

/**
 * Segmented control used in Limits ("Apps" / "Categories"). Replaces M3 TabRow,
 * which is too web-app for the rest of the design language.
 */
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
