package com.coach.screentime.ui.limits

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity
import com.coach.screentime.ui.components.AppGlyph
import com.coach.screentime.ui.components.Eyebrow
import com.coach.screentime.ui.components.SegmentedTabs
import com.coach.screentime.ui.components.UsageBar
import com.coach.screentime.ui.components.brandColorForApp
import com.coach.screentime.ui.components.formatMin
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Alert
import com.coach.screentime.ui.theme.AlertSoft
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.Card2
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.Ink2
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Paper2
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.theme.RuleSoft

@Composable
fun LimitsScreen(viewModel: LimitsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        // Screen header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Eyebrow("Limits", modifier = Modifier.padding(bottom = 4.dp))
                    Text("What you cap.", style = CoachType.headlineMd, color = Ink)
                }
            }
        }

        // Segmented tabs
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SegmentedTabs(
                    tabs = listOf("Apps", "Categories"),
                    selected = tab,
                    onSelect = { tab = it },
                )
            }
        }

        when (tab) {
            0 -> {
                val sorted = state.apps.sortedWith(
                    compareByDescending<AppEntity> { it.isFlagged }.thenBy { it.displayName.lowercase() }
                )
                val flagged = sorted.filter { it.isFlagged }
                val unflagged = sorted.filter { !it.isFlagged }

                if (flagged.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 6.dp).padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Eyebrow("Flagged · ${flagged.size}")
                        }
                    }
                    items(flagged, key = { it.packageName }) { a ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            AppLimitCard(a, viewModel)
                        }
                    }
                }

                if (unflagged.isNotEmpty()) {
                    item {
                        Eyebrow("Not flagged", modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 6.dp))
                    }
                    items(unflagged, key = { it.packageName }) { a ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            AppLimitCard(a, viewModel)
                        }
                    }
                }
            }
            1 -> {
                items(state.categories) { c ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        CategoryLimitCard(c, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLimitCard(a: AppEntity, vm: LimitsViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
            .padding(14.dp, 14.dp, 14.dp, 14.dp)
    ) {
        // App header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppGlyph(displayName = a.displayName, brandColor = brandColorForApp(a.displayName), size = 36)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.displayName, style = CoachType.titleSm, color = Ink)
                Text(
                    a.packageName,
                    style = CoachType.mono.copy(fontSize = 11.sp),
                    color = InkMute,
                )
            }
            Switch(
                checked = a.isFlagged,
                onCheckedChange = { vm.setFlag(a.packageName, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Card, checkedTrackColor = AccentTerracotta),
            )
        }

        if (a.isFlagged) {
            Spacer(Modifier.height(14.dp))
            Divider(color = Rule, thickness = 1.dp, modifier = Modifier.padding(horizontal = 0.dp))
            Spacer(Modifier.height(14.dp))

            Eyebrow("Daily limit", modifier = Modifier.padding(bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Cap number pill
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Card2)
                        .border(1.dp, Rule, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            a.perAppDailyMinutesCap?.toString() ?: "–",
                            style = CoachType.mono.copy(fontSize = 22.sp),
                            color = Ink,
                        )
                        Text("min / day", style = CoachType.meta, color = InkMute)
                    }
                }

                // Limit field (hidden)
                Column(Modifier.weight(1f)) {
                    Text("Set limit (minutes)", style = CoachType.meta, color = InkMute, modifier = Modifier.padding(bottom = 4.dp))
                    MinutesField(
                        value = a.perAppDailyMinutesCap,
                        onChange = { vm.setPerAppCap(a.packageName, it) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Hard lock tile
            val now = System.currentTimeMillis()
            val cooloffEnd = a.hardLockToggleAt + 24 * 60 * 60_000L
            val cooloffActive = !a.hardLockEnabled && a.hardLockToggleAt > 0L && cooloffEnd > now
            val lockBg = if (a.hardLockEnabled) AlertSoft.copy(alpha = 0.35f) else Paper2

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(lockBg)
                    .border(1.dp, if (a.hardLockEnabled) AlertSoft else RuleSoft, RoundedCornerShape(12.dp))
                    .padding(12.dp, 12.dp, 12.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (a.hardLockEnabled) Alert else InkMute,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hard lock at limit",
                        style = CoachType.titleSm.copy(fontSize = 13.5.sp),
                        color = if (a.hardLockEnabled) Alert else Ink,
                    )
                    Text(
                        when {
                            a.hardLockEnabled -> {
                                val remainMs = cooloffEnd - now
                                val h = (remainMs / (60 * 60_000L)).coerceAtLeast(0)
                                val m = ((remainMs % (60 * 60_000L)) / 60_000L).coerceAtLeast(0)
                                "Can't be turned off for ${h}h ${m}m. Ulysses contract."
                            }
                            cooloffActive -> {
                                val remainMs = cooloffEnd - now
                                val h = (remainMs / (60 * 60_000L)).coerceAtLeast(0)
                                val m = ((remainMs % (60 * 60_000L)) / 60_000L).coerceAtLeast(0)
                                "Lock still active. Disables in ${h}h ${m}m."
                            }
                            else -> "No negotiation. Sends you home when the cap hits."
                        },
                        style = CoachType.meta,
                        color = InkMute,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Switch(
                    checked = a.hardLockEnabled,
                    onCheckedChange = { vm.setHardLock(a.packageName, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Card, checkedTrackColor = Alert),
                )
            }
        }
    }
}

@Composable
private fun CategoryLimitCard(c: CategoryEntity, vm: LimitsViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, RuleSoft, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(c.name, style = CoachType.titleMd, color = Ink)
        Spacer(Modifier.height(8.dp))
        MinutesField(
            value = c.dailyMinutesCap,
            onChange = { vm.setCategoryCap(c.id, it) },
            label = "Daily limit (minutes)",
        )
        c.dailyMinutesCap?.let { cap ->
            Spacer(Modifier.height(8.dp))
            Text("${formatMin(cap)} / day", style = CoachType.mono, color = Ink2)
        }
    }
}

@Composable
private fun MinutesField(value: Int?, onChange: (Int?) -> Unit, label: String = "Minutes") {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            text = v.filter { it.isDigit() }.take(4)
            onChange(text.toIntOrNull())
        },
        label = { Text(label, style = CoachType.meta) },
        textStyle = CoachType.mono,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentTerracotta,
            unfocusedBorderColor = Rule,
            focusedContainerColor = Card2,
            unfocusedContainerColor = Card2,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
