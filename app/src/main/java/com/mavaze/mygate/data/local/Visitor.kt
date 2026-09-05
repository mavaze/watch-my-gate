package com.mavaze.mygate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "visitors", indices = [Index(value = ["societyId", "mobile"], unique = true)])
data class Visitor(
    @PrimaryKey val id: String,
    val societyId: Long,
    val name: String,
    val mobile: String,
    val photoRef: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
