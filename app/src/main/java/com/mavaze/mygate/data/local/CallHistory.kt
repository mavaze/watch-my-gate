package com.mavaze.mygate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_history",
    indices = [
        Index(value = ["societyId", "startedAt"]),
        Index(value = ["societyId", "alias", "startedAt"])
    ]
)
data class CallHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val societyId: Long,
    val alias: String,
    val contactResourceName: String?,
    val memberName: String,
    val startedAt: Long,
    val durationSeconds: Long,
    val result: String,
    val connected: Boolean,
    val automaticFailure: Boolean,
    val action: String? = null
)
