package com.mavaze.mygate.data.repository

import com.mavaze.mygate.data.local.AppConfig
import com.mavaze.mygate.data.local.AppConfigDao

class AppConfigRepository(
    private val dao: AppConfigDao
) {
    suspend fun get(): AppConfig {
        return dao.get() ?: AppConfig().also {
            dao.insert(it)
        }
    }

    suspend fun configureKiosk(
        societyId: Long
    ) {
        val current = get()
        dao.update(
            current.copy(
                deviceMode = "KIOSK",
                configuredSocietyId = societyId
            )
        )
    }

    suspend fun configureNormal() {
        val current = get()
        dao.update(
            current.copy(
                deviceMode = "NORMAL",
                configuredSocietyId = null
            )
        )
    }
}
