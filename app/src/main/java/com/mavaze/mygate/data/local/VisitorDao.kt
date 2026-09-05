package com.mavaze.mygate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface VisitorDao {
    @Query("SELECT * FROM visitors WHERE societyId = :societyId AND mobile = :mobile LIMIT 1") suspend fun findByMobile(societyId: Long, mobile: String): Visitor?
    @Query("SELECT * FROM visitors WHERE societyId = :societyId AND id = :id LIMIT 1") suspend fun findById(societyId: Long, id: String): Visitor?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(visitor: Visitor)
    @Query("SELECT * FROM visits WHERE societyId = :societyId AND exitAt IS NULL ORDER BY entryAt ASC") suspend fun activeVisits(societyId: Long): List<Visit>
    @Query("SELECT * FROM visits WHERE societyId = :societyId AND entryAt >= :since ORDER BY entryAt DESC") suspend fun history(societyId: Long, since: Long): List<Visit>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVisit(visit: Visit)
    @Update suspend fun updateVisit(visit: Visit)
    @Query("SELECT * FROM visits WHERE societyId = :societyId AND id = :id LIMIT 1") suspend fun findVisit(societyId: Long, id: String): Visit?
    @Query("DELETE FROM visitors WHERE societyId = :societyId") suspend fun deleteForSociety(societyId: Long)
    @Query("DELETE FROM visits WHERE societyId = :societyId") suspend fun deleteVisitsForSociety(societyId: Long)
}
