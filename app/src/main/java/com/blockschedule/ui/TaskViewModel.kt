package com.blockschedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockschedule.data.TaskEntity
import com.blockschedule.data.TaskRepository
import com.blockschedule.schedule.ScheduledBlock
import com.blockschedule.schedule.Scheduler
import com.blockschedule.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TaskRepository.from(app)

    val allTasks: StateFlow<List<TaskEntity>> =
        repo.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Current minute-of-day, ticking every 30s, for the "now" highlight. */
    val nowMinute: StateFlow<Int> = flow {
        while (true) {
            val t = LocalTime.now()
            emit(t.hour * 60 + t.minute)
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), currentMinute())

    val blocks: StateFlow<List<ScheduledBlock>> =
        combine(allTasks, _selectedDate) { tasks, date ->
            Scheduler.blocksFor(date, tasks)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDate(date: LocalDate) { _selectedDate.value = date }
    fun goToday() { _selectedDate.value = LocalDate.now() }
    fun prevDay() { _selectedDate.value = _selectedDate.value.minusDays(1) }
    fun nextDay() { _selectedDate.value = _selectedDate.value.plusDays(1) }

    fun save(task: TaskEntity) = viewModelScope.launch {
        repo.save(task)
        afterChange()
    }

    fun delete(task: TaskEntity) = viewModelScope.launch {
        repo.delete(task)
        afterChange()
    }

    suspend fun getById(id: Long): TaskEntity? = repo.getById(id)

    fun getByIdAndDelete(id: Long) = viewModelScope.launch {
        repo.getById(id)?.let { repo.delete(it) }
        afterChange()
    }

    /** Keep the widget and reminders in sync after any task change. */
    private suspend fun afterChange() {
        WidgetUpdater.update(getApplication())
        com.blockschedule.reminder.ReminderScheduler.reschedule(getApplication())
    }

    private fun currentMinute(): Int = LocalTime.now().let { it.hour * 60 + it.minute }
}
