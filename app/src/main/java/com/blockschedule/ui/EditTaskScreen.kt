package com.blockschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockschedule.data.Category
import com.blockschedule.data.Frequency
import com.blockschedule.data.TaskEntity
import com.blockschedule.schedule.ScheduledBlock
import com.blockschedule.schedule.Scheduler
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTaskScreen(
    vm: TaskViewModel,
    taskId: Long?,
    onDone: () -> Unit
) {
    val isEditing = taskId != null
    val allTasks by vm.allTasks.collectAsStateWithLifecycle()

    // Form state
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.WORK) }
    var isFixedTime by remember { mutableStateOf(true) }
    var startMinute by remember { mutableStateOf(9 * 60) }
    var durationMinutes by remember { mutableStateOf(60) }
    var frequency by remember { mutableStateOf(Frequency.DAILY) }
    var weekdays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var windowStart by remember { mutableStateOf(8 * 60) }
    var windowEnd by remember { mutableStateOf(21 * 60) }
    var count by remember { mutableStateOf(3) }
    var parentId by remember { mutableStateOf<Long?>(null) }
    var loaded by remember { mutableStateOf(taskId == null) }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            vm.getById(taskId)?.let { t ->
                title = t.title
                category = t.category
                isFixedTime = t.isFixedTime
                startMinute = t.startMinute
                durationMinutes = t.durationMinutes
                frequency = t.frequency
                weekdays = TaskEntity.weekdaysFromMask(t.daysOfWeek).toSet()
                anchorDate = LocalDate.ofEpochDay(t.anchorEpochDay)
                windowStart = t.windowStartMinute
                windowEnd = t.windowEndMinute
                count = t.count
                parentId = t.parentId
            }
            loaded = true
        }
    }

    // Tasks that can be a parent block for a sub-block (fixed-time, top-level, not this task).
    val parentOptions = allTasks.filter {
        it.parentId == null && it.isFixedTime &&
            it.frequency != Frequency.TIMES_PER_DAY && it.id != (taskId ?: -1L)
    }
    val isSub = parentId != null
    val parentTitle = allTasks.firstOrNull { it.id == parentId }?.title ?: "the block"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit task" else "New task") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            vm.getByIdAndDelete(taskId!!)
                            onDone()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete task")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            EnumDropdown(
                label = "Category",
                selected = category,
                options = Category.entries,
                optionLabel = { it.label },
                onSelected = { category = it }
            )

            // Optional: nest this inside an existing fixed block (e.g. lunch within work).
            if (parentOptions.isNotEmpty() || isSub) {
                ParentDropdown(parents = parentOptions, selectedId = parentId) { parentId = it }
            }

            val effectiveFixed = if (frequency == Frequency.TIMES_PER_DAY) false else isFixedTime

            SectionLabel("When")
            if (isSub) {
                HelperText("Happens inside “$parentTitle”, on the same days it occurs.")
                TimeRow("Starts at", startMinute) { startMinute = it }
                DurationRow(durationMinutes) { durationMinutes = it }
                HelperText("Ends at ${ScheduledBlock.formatTime(startMinute + durationMinutes)}")
            } else {
                if (frequency != Frequency.TIMES_PER_DAY) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isFixedTime,
                            onClick = { isFixedTime = true },
                            label = { Text("Set start time") }
                        )
                        FilterChip(
                            selected = !isFixedTime,
                            onClick = { isFixedTime = false },
                            label = { Text("Flexible (auto-placed)") }
                        )
                    }
                }
                if (effectiveFixed) {
                    TimeRow("Starts at", startMinute) { startMinute = it }
                    DurationRow(durationMinutes) { durationMinutes = it }
                    HelperText("Ends at ${ScheduledBlock.formatTime(startMinute + durationMinutes)}")
                } else {
                    DurationRow(durationMinutes) { durationMinutes = it }
                    HelperText("Fit anywhere between:")
                    TimeRow("Earliest", windowStart) { windowStart = it }
                    TimeRow("Latest", windowEnd) { windowEnd = it }
                }
            }

            if (!isSub) {
                SectionLabel("Repeat")
                EnumDropdown(
                    label = "Frequency",
                    selected = frequency,
                    options = Frequency.entries,
                    optionLabel = { it.label },
                    onSelected = { frequency = it }
                )

                when (frequency) {
                    Frequency.WEEKLY, Frequency.BIWEEKLY -> WeekdayPicker(weekdays) { weekdays = it }
                    Frequency.TIMES_PER_WEEK -> CountRow(count, "a week", 1, 7) { count = it }
                    Frequency.TIMES_PER_DAY -> CountRow(count, "a day", 1, 12) { count = it }
                    else -> {}
                }

                DateRow(
                    label = if (frequency == Frequency.ONCE) "Date" else "Starts on",
                    date = anchorDate
                ) { anchorDate = it }

                when (frequency) {
                    Frequency.MONTHLY ->
                        HelperText("Repeats on the ${anchorDate.dayOfMonth.ordinal()} of each month.")
                    Frequency.YEARLY ->
                        HelperText("Repeats every ${anchorDate.format(DateTimeFormatter.ofPattern("MMMM d"))}.")
                    Frequency.TIMES_PER_WEEK -> HelperText(
                        "Lands on: " + Scheduler.spreadDays(count).sortedBy { it.value }
                            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
                    )
                    Frequency.TIMES_PER_DAY -> HelperText(
                        "$count evenly-spread times between " +
                            "${ScheduledBlock.formatTime(windowStart)} and ${ScheduledBlock.formatTime(windowEnd)}."
                    )
                    else -> {}
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    // Sub-blocks are always fixed-time; "X times a day" is always flexible.
                    val finalFixed = when {
                        isSub -> true
                        frequency == Frequency.TIMES_PER_DAY -> false
                        else -> isFixedTime
                    }
                    val entity = TaskEntity(
                        id = taskId ?: 0L,
                        title = title.trim(),
                        category = category,
                        isFixedTime = finalFixed,
                        startMinute = startMinute,
                        durationMinutes = durationMinutes.coerceAtLeast(5),
                        frequency = frequency,
                        daysOfWeek = TaskEntity.weekdayMask(weekdays),
                        count = count,
                        parentId = parentId,
                        anchorEpochDay = anchorDate.toEpochDay(),
                        windowStartMinute = windowStart,
                        windowEndMinute = windowEnd,
                        enabled = true
                    )
                    vm.save(entity)
                    onDone()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isEditing) "Save changes" else "Add task", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Int.ordinal(): String {
    val suffix = when {
        this in 11..13 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$this$suffix"
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun HelperText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentDropdown(
    parents: List<TaskEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = parents.firstOrNull { it.id == selectedId }?.title ?: "Nothing (standalone task)"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Part of (sub-block)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Nothing (standalone task)") },
                onClick = { onSelect(null); expanded = false }
            )
            parents.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.title) },
                    onClick = { onSelect(p.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun CountRow(value: Int, unit: String, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("How often", modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }) { Text("–") }
        Text(
            "$value× $unit",
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(max)) }) { Text("+") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayPicker(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { day ->
            val isOn = day in selected
            FilterChip(
                selected = isOn,
                onClick = {
                    onChange(if (isOn) selected - day else selected + day)
                },
                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
            )
        }
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, onChange: (Int) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = { show = true }) {
            Text(ScheduledBlock.formatTime(minute))
        }
    }
    if (show) {
        TimePickerModal(
            initialMinute = minute,
            onConfirm = { onChange(it); show = false },
            onDismiss = { show = false }
        )
    }
}

@Composable
private fun DurationRow(minutes: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Duration", modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = { onChange((minutes - 15).coerceAtLeast(5)) }) { Text("–") }
        Text(
            formatDuration(minutes),
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        OutlinedButton(onClick = { onChange((minutes + 15).coerceAtMost(24 * 60)) }) { Text("+") }
    }
}

private fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(initialMinute: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = (initialMinute / 60).coerceIn(0, 23),
        initialMinute = initialMinute % 60,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        }
    )
}

@Composable
private fun DateRow(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        AssistChip(
            onClick = { show = true },
            label = { Text(date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))) }
        )
    }
    if (show) {
        DatePickerModal(
            initial = date,
            onConfirm = { onChange(it); show = false },
            onDismiss = { show = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(initial: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * 86_400_000L
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
