package com.blockschedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockschedule.data.TaskEntity
import com.blockschedule.data.TaskRepository
import com.blockschedule.game.CompletionManager
import com.blockschedule.game.GameEvent
import com.blockschedule.game.GamePrefs
import com.blockschedule.game.GameState
import com.blockschedule.schedule.ScheduledBlock
import com.blockschedule.schedule.Scheduler
import com.blockschedule.widget.WidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    // --- gamification ---

    private val game = GamePrefs(app)

    @OptIn(ExperimentalCoroutinesApi::class)
    val completions: StateFlow<Map<Long, Int>> =
        _selectedDate
            .flatMapLatest { date -> repo.observeCompletions(date.toEpochDay()) }
            .map { list -> list.associate { it.taskId to it.doneCount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** (completed, total) for the selected day. */
    val progress: StateFlow<Pair<Int, Int>> =
        combine(blocks, completions) { b, c -> Scheduler.progress(b, c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    private val _gameState = MutableStateFlow(readGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GameEvent> = _events

    init {
        // Refresh points/streak whenever completion data changes (incl. widget toggles).
        viewModelScope.launch { completions.collect { _gameState.value = readGameState() } }
    }

    // Tracks rapid completions for the playful "brat check".
    private val recentCompletions = ArrayDeque<Long>()
    private var lastBratAt = 0L
    private var lastCompletedBlock: ScheduledBlock? = null

    fun toggleComplete(block: ScheduledBlock) = viewModelScope.launch {
        val result = CompletionManager.toggle(
            getApplication(), block.taskId, block.instanceIndex, _selectedDate.value.toEpochDay()
        )
        _gameState.value = readGameState()
        if (result.nowDone) {
            _events.tryEmit(GameEvent.PointsEarned(result.pointsDelta))
            lastCompletedBlock = block
            maybeBratCheck()
        }
        result.unlocked.forEach { _events.tryEmit(GameEvent.AchievementUnlocked(it)) }
        result.evolvedForm?.let { _events.tryEmit(GameEvent.BuddyEvolved(it)) }
        when {
            result.dayJustCompleted -> _events.tryEmit(GameEvent.DanceParty("All done today!"))
            result.taskParty -> _events.tryEmit(GameEvent.DanceParty("${block.title} done!"))
        }
    }

    /** If she's speed-tapping a bunch of tasks, call her out (playfully). */
    private fun maybeBratCheck() {
        val now = System.currentTimeMillis()
        recentCompletions.addLast(now)
        while (recentCompletions.isNotEmpty() && now - recentCompletions.first() > 5_000) {
            recentCompletions.removeFirst()
        }
        if (recentCompletions.size >= 4 && now - lastBratAt > 20_000) {
            lastBratAt = now
            recentCompletions.clear()
            _events.tryEmit(GameEvent.BratCheck)
        }
    }

    /** Undo the most recent completion (used when she admits she didn't really do it). */
    fun undoLastCompletion() {
        val b = lastCompletedBlock ?: return
        if (!Scheduler.isDone(b, completions.value)) return
        viewModelScope.launch {
            CompletionManager.toggle(
                getApplication(), b.taskId, b.instanceIndex, _selectedDate.value.toEpochDay()
            )
            _gameState.value = readGameState()
        }
    }

    fun isDone(block: ScheduledBlock): Boolean = Scheduler.isDone(block, completions.value)

    private fun readGameState() = GameState(
        points = game.points, level = game.level, levelProgress = game.levelProgress,
        pointsPerLevel = game.pointsPerLevel, streak = game.streak, bestStreak = game.bestStreak,
        totalCompleted = game.totalCompleted, perfectDays = game.perfectDays, unlocked = game.unlocked
    )

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
