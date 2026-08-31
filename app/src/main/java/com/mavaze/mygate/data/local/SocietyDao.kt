package com.mavaze.mygate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SocietyDao {

    @Query("""
        SELECT * FROM societies
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun findById(id: Long): Society?

    @Query("""
        SELECT * FROM societies
        ORDER BY name
    """)
    suspend fun getAll(): List<Society>

    @Query("""
        SELECT * FROM societies
        WHERE adminEmail = :email
        LIMIT 1
    """)
    suspend fun findByAdminEmail(
        email: String
    ): Society?

    @Insert
    suspend fun insert(
        society: Society
    ): Long

    @Update
    suspend fun update(
        society: Society
    )

    @Query("""
        DELETE FROM societies
        WHERE id = :id
    """)
    suspend fun deleteById(
        id: Long
    )
}