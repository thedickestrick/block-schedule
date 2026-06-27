package com.blockschedule.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper over the DAO. Centralizes data access so the ViewModel and the
 * widget worker share one path, and so widget refreshes happen on every write.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val completionDao: CompletionDao
) {

    val allTasks: Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun enabledTasks(): List<TaskEntity> = dao.getEnabled()

    suspend fun getById(id: Long): TaskEntity? = dao.getById(id)

    suspend fun save(task: TaskEntity): Long =
        if (task.id == 0L) dao.insert(task) else { dao.update(task); task.id }

    suspend fun delete(task: TaskEntity) {
        completionDao.clearTask(task.id)
        dao.delete(task)
    }

    // --- completions (gamification) ---

    fun observeCompletions(day: Long): Flow<List<CompletionEntity>> =
        completionDao.observeForDate(day)

    suspend fun getCompletions(day: Long): List<CompletionEntity> =
        completionDao.getForDate(day)

    suspend fun setDoneCount(taskId: Long, day: Long, count: Int) {
        if (count <= 0) completionDao.clear(taskId, day)
        else completionDao.upsert(CompletionEntity(taskId, day, count))
    }

    companion object {
        fun from(context: Context): TaskRepository {
            val db = AppDatabase.get(context)
            return TaskRepository(db.taskDao(), db.completionDao())
        }
    }
}
