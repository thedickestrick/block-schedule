package com.blockschedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockschedule.schedule.ScheduledBlock
import com.blockschedule.schedule.Scheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    vm: TaskViewModel,
    updateVm: com.blockschedule.update.UpdateViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onManageTasks: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val date by vm.selectedDate.collectAsStateWithLifecycle()
    val nowMinute by vm.nowMinute.collectAsStateWithLifecycle()
    val isToday = date == LocalDate.now()
    val currentIndex = if (isToday) Scheduler.currentBlockIndex(blocks, nowMinute) else -1

    Scaffold(
        topBar = {
            val context = androidx.compose.ui.platform.LocalContext.current
            TopAppBar(
                title = { Text("Block Schedule", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        val ok = com.blockschedule.widget.WidgetPinner.requestPin(context)
                        if (!ok) {
                            android.widget.Toast.makeText(
                                context,
                                "Add the widget from your home screen's widget menu.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Add widget to home screen")
                    }
                    IconButton(onClick = onManageTasks) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All tasks")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            DateBar(
                date = date,
                isToday = isToday,
                onPrev = vm::prevDay,
                onNext = vm::nextDay,
                onToday = vm::goToday
            )

            UpdateBanner(updateVm)

            if (blocks.isEmpty()) {
                EmptyState(onAddTask)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blocks, key = { "${it.taskId}-${it.startMinute}-${it.continuedFromYesterday}" }) { block ->
                        val idx = blocks.indexOf(block)
                        BlockCard(
                            block = block,
                            isCurrent = idx == currentIndex,
                            onClick = { onEditTask(block.taskId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateBar(
    date: LocalDate,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isToday) "Today" else date.format(DateTimeFormatter.ofPattern("EEEE")),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isToday) {
                TextButton(onClick = onToday) { Text("Jump to today") }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun BlockCard(
    block: ScheduledBlock,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val catColor = Color(block.category.colorArgb)
    val bg = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        block.unscheduled -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(6.dp).height(44.dp).clip(RoundedCornerShape(3.dp)).background(catColor)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (block.hasConflict) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Overlaps another task",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            val sub = when {
                block.unscheduled -> "No free time today — adjust its window"
                else -> buildString {
                    append(ScheduledBlock.formatRange(block.startMinute, block.endMinute))
                    append("  ·  ")
                    append(block.category.label)
                    if (block.isFlexible) append("  ·  flexible")
                    if (block.continuedFromYesterday) append("  ·  from yesterday")
                    if (block.continuesTomorrow) append("  ·  into tomorrow")
                }
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isCurrent) {
            Spacer(Modifier.width(8.dp))
            NowPill()
        }
    }
}

@Composable
private fun NowPill() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "NOW",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyState(onAddTask: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Nothing scheduled",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Add work, sleep, the gym, appointments — recurring ones get scheduled automatically every day.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onAddTask) { Text("Add your first task") }
        }
    }
}
