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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.runtime.setValue
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
    val completions by vm.completions.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val game by vm.gameState.collectAsStateWithLifecycle()
    val isToday = date == LocalDate.now()
    val currentKey = if (isToday) Scheduler.activeLeafKey(blocks, nowMinute) else null

    // Celebration feedback
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var confettiTrigger by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var floatMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is com.blockschedule.game.GameEvent.PointsEarned ->
                    floatMessage = "+${event.delta} ⭐"
                com.blockschedule.game.GameEvent.DayCompleted -> {
                    confettiTrigger++
                    floatMessage = "Day complete! 🎉"
                }
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(floatMessage) {
        if (floatMessage != null) {
            kotlinx.coroutines.delay(1400)
            floatMessage = null
        }
    }

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
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
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
                    GameHeader(done = progress.first, total = progress.second, game = game)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 8.dp, bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(blocks, key = { "${it.taskId}-${it.startMinute}-${it.continuedFromYesterday}" }) { block ->
                            BlockCard(
                                block = block,
                                currentKey = currentKey,
                                isDone = { Scheduler.isDone(it, completions) },
                                onToggleDone = {
                                    haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                    )
                                    vm.toggleComplete(it)
                                },
                                onEdit = onEditTask
                            )
                        }
                    }
                }
            }

            // Celebration overlays
            FloatingPoints(floatMessage)
            ConfettiOverlay(confettiTrigger)
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
    currentKey: String?,
    isDone: (ScheduledBlock) -> Boolean,
    onToggleDone: (ScheduledBlock) -> Unit,
    onEdit: (Long) -> Unit
) {
    val isCurrent = block.key == currentKey
    val done = !block.unscheduled && isDone(block)
    val bg = when {
        done -> MaterialTheme.colorScheme.surfaceVariant
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        block.unscheduled -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(block.taskId) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(6.dp).height(44.dp).clip(RoundedCornerShape(3.dp))
                    .background(Color(block.category.colorArgb).copy(alpha = if (done) 0.4f else 1f))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (block.hasConflict && !done) {
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
            if (isCurrent && !done) {
                Spacer(Modifier.width(8.dp))
                NowPill()
            }
            if (!block.unscheduled) {
                Spacer(Modifier.width(8.dp))
                CompleteCircle(done = done, onClick = { onToggleDone(block) })
            }
        }

        // Nested sub-blocks (e.g. a lunch break inside work).
        block.children.forEach { child ->
            ChildRow(
                child = child,
                isCurrent = child.key == currentKey,
                done = isDone(child),
                onToggleDone = { onToggleDone(child) },
                onClick = { onEdit(child.taskId) }
            )
        }
    }
}

@Composable
private fun ChildRow(
    child: ScheduledBlock,
    isCurrent: Boolean,
    done: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 30.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("↳", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.width(4.dp).height(34.dp).clip(RoundedCornerShape(2.dp))
                .background(Color(child.category.colorArgb).copy(alpha = if (done) 0.4f else 1f))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = child.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = when {
                    done -> MaterialTheme.colorScheme.onSurfaceVariant
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ScheduledBlock.formatRange(child.startMinute, child.endMinute) + "  ·  break",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isCurrent && !done) NowPill()
        Spacer(Modifier.width(8.dp))
        CompleteCircle(done = done, onClick = onToggleDone)
    }
}

@Composable
private fun CompleteCircle(done: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        if (done) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Completed — tap to undo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        } else {
            Icon(
                Icons.Outlined.Circle,
                contentDescription = "Mark complete",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(30.dp)
            )
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
