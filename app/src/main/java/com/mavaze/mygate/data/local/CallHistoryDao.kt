package com.mavaze.mygate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CallHistoryDao {
    @Insert suspend fun insert(entry: CallHistory)
    @Query("SELECT * FROM call_history WHERE societyId = :societyId AND startedAt >= :since ORDER BY startedAt DESC")
    suspend fun recent(societyId: Long, since: Long): List<CallHistory>
}
