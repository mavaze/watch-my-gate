package com.mavaze.mygate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey
    val id: Int = 1,

    /**
     * True after the factory default admin password
     * has been changed.
     */
    val defaultAdminPasswordChanged: Boolean = false,

    /**
     * Later this will distinguish a normal installation
     * from the dedicated Watchman/kiosk phone.
     */
    val deviceMode: String = "NORMAL",

    val configuredSocietyId: Long? = null
)