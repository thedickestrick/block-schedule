package com.blockschedule.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE epochDay = :day")
    fun observeForDate(day: Long): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE epochDay = :day")
    suspend fun getForDate(day: Long): List<CompletionEntity>

    @Upsert
    suspend fun upsert(completion: CompletionEntity)

    @Query("DELETE FROM completions WHERE taskId = :taskId AND epochDay = :day")
    suspend fun clear(taskId: Long, day: Long)

    @Query("DELETE FROM completions WHERE taskId = :taskId")
    suspend fun clearTask(taskId: Long)
}
