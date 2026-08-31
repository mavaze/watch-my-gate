package com.mavaze.mygate

import android.app.Application
import com.mavaze.mygate.auth.PasswordHasher
import com.mavaze.mygate.data.local.AppConfig
import com.mavaze.mygate.data.local.AppDatabase
import com.mavaze.mygate.data.local.AuthType
import com.mavaze.mygate.data.local.User
import com.mavaze.mygate.data.local.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyGateApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            bootstrap()
        }
    }

    private suspend fun bootstrap() {

        val userDao = database.userDao()
        val configDao = database.appConfigDao()

        var config = configDao.get()

        if (config == null) {
            config = AppConfig()

            configDao.insert(config)
        }

        val defaultAdminCount =
            userDao.countDefaultAdmins()

        if (defaultAdminCount == 0) {

            val defaultAdmin = User(
                username = "admin",
                displayName = "Default Administrator",
                societyId = null,
                role = UserRole.DEFAULT_ADMIN,
                authType = AuthType.LOCAL,
                passwordHash = PasswordHasher.hash(
                    "admin"
                ),
                mustChangePassword = true
            )

            userDao.insert(defaultAdmin)
        }
    }
}