package com.mavaze.mygate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GateContactDao {

    @Query("""
        SELECT * FROM gate_contacts
        WHERE societyId = :societyId
          AND alias IS NOT NULL
          AND TRIM(alias) != ''
          AND priority > 0
        ORDER BY alias COLLATE NOCASE, priority ASC, displayName COLLATE NOCASE
    """)
    suspend fun getCallableForSociety(
        societyId: Long
    ): List<GateContact>

    @Query("""
        SELECT * FROM gate_contacts
        WHERE societyId = :societyId
          AND alias = :alias
          AND priority > 0
        ORDER BY priority ASC, displayName COLLATE NOCASE
    """)
    suspend fun getForAlias(
        societyId: Long,
        alias: String
    ): List<GateContact>

    @Query("""
        SELECT * FROM gate_contacts
        WHERE societyId = :societyId
          AND id = :id
        LIMIT 1
    """)
    suspend fun findById(
        societyId: Long,
        id: Long
    ): GateContact?

    @Query("""
        SELECT * FROM gate_contacts
        WHERE societyId = :societyId
          AND googleResourceName = :resourceName
        LIMIT 1
    """)
    suspend fun find(
        societyId: Long,
        resourceName: String
    ): GateContact?

    @Insert
    suspend fun insertAll(
        contacts: List<GateContact>
    )

    @Transaction
    suspend fun replaceForSociety(
        societyId: Long,
        contacts: List<GateContact>
    ) {
        deleteForSociety(societyId)
        if (contacts.isNotEmpty()) {
            insertAll(contacts)
        }
    }

    @Query("""
        DELETE FROM gate_contacts
        WHERE societyId = :societyId
    """)
    suspend fun deleteForSociety(
        societyId: Long
    )
}
