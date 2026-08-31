package com.mavaze.mygate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {

    @Query("""
        SELECT * FROM users
        WHERE username = :username
        LIMIT 1
    """)
    suspend fun findByUsername(username: String): User?

    @Query("""
        SELECT * FROM users
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun findById(id: Long): User?

    @Query("""
        SELECT * FROM users
        WHERE societyId = :societyId
        AND role = 'WATCHMAN'
        ORDER BY username
    """)
    suspend fun getWatchmen(societyId: Long): List<User>

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("""
        UPDATE users
        SET googleAuthorized = :authorized
        WHERE id = :userId
    """)
    suspend fun setGoogleAuthorized(
        userId: Long,
        authorized: Boolean
    )

    @Query("""
        UPDATE users
        SET enabled = :enabled
        WHERE societyId = :societyId
    """)
    suspend fun setEnabledForSociety(
        societyId: Long,
        enabled: Boolean
    )

    @Query("""
        DELETE FROM users
        WHERE societyId = :societyId
    """)
    suspend fun deleteBySocietyId(
        societyId: Long
    )

    @Query("""
        DELETE FROM users
        WHERE id = :id
    """)
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT COUNT(*) FROM users
        WHERE role = 'DEFAULT_ADMIN'
    """)
    suspend fun countDefaultAdmins(): Int
}
