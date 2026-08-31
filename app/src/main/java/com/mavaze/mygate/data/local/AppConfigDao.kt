package com.mavaze.mygate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppConfigDao {

    @Query("""
        SELECT * FROM app_config
        WHERE id = 1
        LIMIT 1
    """)
    suspend fun get(): AppConfig?

    @Insert
    suspend fun insert(config: AppConfig)

    @Update
    suspend fun update(config: AppConfig)
}