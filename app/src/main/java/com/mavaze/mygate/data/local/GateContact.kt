package com.mavaze.mygate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gate_contacts",
    indices = [
        Index(
            value = ["societyId", "googleResourceName"],
            unique = true
        ),
        Index(value = ["societyId"]),
        Index(value = ["societyId", "alias", "priority"])
    ]
)
data class GateContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val societyId: Long,
    val googleResourceName: String,
    val displayName: String,
    val phoneNumbersJson: String,
    val alias: String?,
    val priority: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
