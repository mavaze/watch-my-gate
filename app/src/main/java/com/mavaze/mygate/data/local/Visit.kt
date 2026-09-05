package com.mavaze.mygate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "visits", indices = [Index(value = ["societyId", "entryAt"]), Index(value = ["societyId", "visitorId", "exitAt"])])
data class Visit(
    @PrimaryKey val id: String,
    val societyId: Long,
    val visitorId: String,
    val alias: String,
    val entryAt: Long,
    val exitAt: Long? = null,
    val approvedBy: String,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis()
)
