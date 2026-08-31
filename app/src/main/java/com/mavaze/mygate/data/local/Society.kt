package com.mavaze.mygate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "societies",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["adminEmail"], unique = true)
    ]
)
data class Society(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val adminEmail: String,
    val logoPath: String? = null,
    val enabled: Boolean = true,
    val cloudMetadataDirty: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
