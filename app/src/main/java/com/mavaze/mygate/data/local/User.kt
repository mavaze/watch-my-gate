package com.mavaze.mygate.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val username: String,
    val displayName: String,
    val societyId: Long? = null,
    val role: UserRole,
    val authType: AuthType,
    val passwordHash: String? = null,
    val mustChangePassword: Boolean = false,
    val enabled: Boolean = true,
    val googleAuthorized: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
