package com.blockschedule.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper over the DAO. Centralizes data access so the ViewModel and the
 * widget worker share one path, and so widget refreshes happen on every write.
 */
class TaskRepository(private val dao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun enabledTasks(): List<TaskEntity> = dao.getEnabled()

    suspend fun getById(id: Long): TaskEntity? = dao.getById(id)

    suspend fun save(task: TaskEntity): Long =
        if (task.id == 0L) dao.insert(task) else { dao.update(task); task.id }

    suspend fun delete(task: TaskEntity) = dao.delete(task)

    companion object {
        fun from(context: Context): TaskRepository =
            TaskRepository(AppDatabase.get(context).taskDao())
    }
}
