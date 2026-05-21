package com.coach.screentime.ui.today

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.focus.FocusActivity
import com.coach.screentime.ui.components.AdherenceRing
import com.coach.screentime.ui.components.CoachAvatar
import com.coach.screentime.ui.components.Eyebrow
import com.coach.screentime.ui.components.Pill
import com.coach.screentime.ui.components.PunishmentBanner
import com.coach.screentime.ui.components.UsageRow
import com.coach.screentime.ui.components.brandColorForApp
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.RuleSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        // Header
        item {
            ScreenHeader(
                eyebrow = todayEyebrow(),
                title   = greeting(),
                onFocusClick = { context.startActivity(Intent(context, FocusActivity::class.java)) },
            )
        }

        // Punishment banner
        state.activePunishment?.let { p ->
            item {
                Box(Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)) {
                    PunishmentBanner(p)
                }
            }
        }

        // Hero card
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HeroCard(state)
            }
        }

        // Coach line
        if (state.totalMinutes > 0) {
            item {
                CoachLine(state)
            }
        }

        // Categories section
        if (state.categories.any { it.minutes > 0 || it.capMinutes != null }) {
            item {
                Eyebrow(
                    "Categories",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(top = 6.dp),
                )
            }
            item {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card)
                        .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
                ) {
                    Column {
                        val cats = state.categories.filter { it.minutes > 0 || it.capMinutes != null }
                        cats.forEachIndexed { i, c ->
                            if (i > 0) Divider(color = RuleSoft, thickness = 1.dp)
                            CategoryRow(c)
                        }
                    }
                }
            }
        }

        // Apps section header
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow("Apps · top today")
                Spacer(Modifier.width(6.dp))
                Text("${state.apps.size}", style = CoachType.meta, color = InkMute)
            }
        }

        // Apps list card
        item {
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Card)
                    .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
            ) {
                Column {
                    state.apps.forEachIndexed { i, a ->
                        if (i > 0) Divider(color = RuleSoft, thickness = 1.dp)
                        UsageRow(
                            label = a.label,
                            minutes = a.minutes,
                            opens = a.opens,
                            capMinutes = a.capMinutes,
                            packageName = a.packageName,
                            deltaVsWeekAvgPct = a.deltaVsWeekAvgPct,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(eyebrow: String, title: String, onFocusClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Eyebrow(eyebrow, modifier = Modifier.padding(bottom = 4.dp))
            Text(title, style = CoachType.headlineMd, color = Ink)
        }
        Pill(
            text = "Focus",
            background = Card,
            border = RuleSoft,
            textColor = Ink2,
        )
    }
}

@Composable
private fun HeroCard(state: TodayUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .border(1.dp, RuleSoft, RoundedCornerShape(18.dp))
            .padding(18.dp, 18.dp, 18.dp, 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AdherenceRing(adherence = state.adherence, sizeDp = 132)
            Spacer(Modifier.width(16.dp))
            Column {
                Eyebrow("So far today", modifier = Modifier.padding(bottom = 4.dp))
                Text(
                    formatMin(state.totalMinutes),
                    style = CoachType.headlineLg.copy(fontSize = 36.sp),
                    color = Ink,
                )
                val goalMin = state.categories.mapNotNull { it.capMinutes }.sum().takeIf { it > 0 }
                Text(
                    "${state.totalOpens} opens${if (goalMin != null) " · goal ${formatMin(goalMin)}" else ""}",
                    style = CoachType.meta,
                    color = InkMute,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (state.streak > 0) {
                        Pill("${state.streak}-day streak", textColor = AccentTerracotta, border = AccentTerracotta.copy(alpha = 0.3f))
                    }
                    state.worstHourLabel?.let { label ->
                        Pill("Worst hour $label")
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachLine(state: TodayUiState) {
    val line = buildCoachLine(state) ?: return
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CoachAvatar(size = 22)
        Column {
            Eyebrow("Coach", modifier = Modifier.padding(bottom = 2.dp))
            Text(
                line,
                style = CoachType.headlineSm.copy(fontStyle = FontStyle.Italic, fontSize = 17.sp),
                color = Ink,
            )
        }
    }
}

@Composable
private fun CategoryRow(c: CategoryRow) {
    val over = c.capMinutes != null && c.minutes > c.capMinutes
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(c.name, style = CoachType.titleSm, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatMin(c.minutes),
                    style = CoachType.mono,
                    color = if (over) AccentTerracotta else Ink2,
                )
                c.capMinutes?.let {
                    Text("/ ${formatMin(it)}", style = CoachType.meta, color = InkMute)
                }
            }
        }
        c.capMinutes?.let { cap ->
            com.coach.screentime.ui.components.UsageBar(used = c.minutes, cap = cap)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${c.opens} opens · ${if (over) "over by ${formatMin(c.minutes - (c.capMinutes ?: c.minutes))}" else "on track"}",
            style = CoachType.meta,
            color = if (over) AccentTerracotta else InkMute,
        )
    }
}

private fun buildCoachLine(state: TodayUiState): String? {
    val overCats = state.categories.filter { it.capMinutes != null && it.minutes > it.capMinutes }
    if (overCats.isNotEmpty()) {
        val c = overCats.first()
        val overBy = c.minutes - (c.capMinutes ?: c.minutes)
        return "You're ${formatMin(overBy)} over on ${c.name}."
    }
    return null
}

private fun todayEyebrow(): String {
    val now = LocalDate.now()
    val day = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
    val month = now.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
    return "$day · $month ${now.dayOfMonth}"
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Morning."
        hour < 17 -> "Afternoon."
        else      -> "Evening."
    }
}

// Expose formatMin for internal use in this file
private fun formatMin(min: Int): String {
    if (min < 60) return "${min}m"
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}
