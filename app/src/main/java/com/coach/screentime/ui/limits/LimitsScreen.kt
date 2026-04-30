package com.coach.screentime.ui.limits

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coach.screentime.data.db.entities.AppEntity
import com.coach.screentime.data.db.entities.CategoryEntity

@Composable
fun LimitsScreen(viewModel: LimitsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Apps") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Categories") })
        }
        when (tab) {
            0 -> AppsTab(state.apps, viewModel)
            1 -> CategoriesTab(state.categories, viewModel)
        }
    }
}

@Composable
private fun AppsTab(apps: List<AppEntity>, vm: LimitsViewModel) {
    val sorted = apps.sortedWith(compareByDescending<AppEntity> { it.isFlagged }.thenBy { it.displayName.lowercase() })
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sorted, key = { it.packageName }) { a -> AppRow(a, vm) }
    }
}

@Composable
private fun AppRow(a: AppEntity, vm: LimitsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(a.displayName, fontWeight = FontWeight.SemiBold)
                    Text(a.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = a.isFlagged,
                    onCheckedChange = { vm.setFlag(a.packageName, it) },
                )
            }
            if (a.isFlagged) {
                Spacer(Modifier.height(8.dp))
                MinutesField(
                    label = "Daily limit (min)",
                    value = a.perAppDailyMinutesCap,
                    onChange = { vm.setPerAppCap(a.packageName, it) },
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hard lock at limit", modifier = Modifier.weight(1f))
                    Switch(
                        checked = a.hardLockEnabled,
                        onCheckedChange = { vm.setHardLock(a.packageName, it) },
                    )
                }
                if (a.hardLockEnabled) {
                    Text(
                        "Cannot be disabled until 24h after toggling.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesTab(categories: List<CategoryEntity>, vm: LimitsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { c ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(c.name, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    MinutesField(
                        label = "Daily limit (min)",
                        value = c.dailyMinutesCap,
                        onChange = { vm.setCategoryCap(c.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MinutesField(label: String, value: Int?, onChange: (Int?) -> Unit) {
    var text by remember(value) { androidx.compose.runtime.mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            text = v.filter { it.isDigit() }.take(4)
            onChange(text.toIntOrNull())
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
