package com.mavaze.mygate.data.repository

import com.mavaze.mygate.auth.PasswordHasher
import com.mavaze.mygate.data.local.AppDatabase
import com.mavaze.mygate.data.local.AuthType
import com.mavaze.mygate.data.local.Society
import com.mavaze.mygate.data.local.User
import com.mavaze.mygate.data.local.UserRole

class SocietyRepository(
    private val database: AppDatabase
) {
    suspend fun getAll(): List<Society> =
        database.societyDao().getAll()

    suspend fun findById(id: Long): Society? =
        database.societyDao().findById(id)

    suspend fun findByAdminEmail(email: String): Society? =
        database.societyDao().findByAdminEmail(email.trim().lowercase())

    suspend fun getWatchmen(societyId: Long): List<User> =
        database.userDao().getWatchmen(societyId)

    suspend fun createSociety(
        name: String,
        adminEmail: String,
        logoPath: String? = null
    ): Long {
        require(name.isNotBlank()) {
            "Society name cannot be empty"
        }

        val normalizedEmail = adminEmail.trim().lowercase()
        require(normalizedEmail.endsWith("@gmail.com")) {
            "Society administrator must use a Gmail address"
        }

        if (database.societyDao().findByAdminEmail(normalizedEmail) != null) {
            throw IllegalArgumentException(
                "A society already exists for this Gmail address"
            )
        }

        val society = Society(
            name = name.trim(),
            adminEmail = normalizedEmail,
            logoPath = logoPath
        )

        val admin = User(
            username = normalizedEmail,
            displayName = name.trim(),
            societyId = null,
            role = UserRole.SOCIETY_ADMIN,
            authType = AuthType.GOOGLE,
            passwordHash = null,
            mustChangePassword = false,
            enabled = true,
            googleAuthorized = false
        )

        return database.createSocietyWithAdmin(society, admin)
    }

    suspend fun createWatchman(
        societyId: Long,
        username: String,
        displayName: String,
        temporaryPassword: String
    ): Long {
        database.societyDao().findById(societyId)
            ?: throw IllegalArgumentException("Society not found")

        return UserRepository(database.userDao()).createWatchman(
            societyId,
            username,
            displayName,
            temporaryPassword
        )
    }

    suspend fun setEnabled(
        societyId: Long,
        enabled: Boolean
    ) {
        val society = database.societyDao().findById(societyId)
            ?: throw IllegalArgumentException("Society not found")

        database.societyDao().update(society.copy(enabled = enabled))
        database.userDao().setEnabledForSociety(
            societyId,
            enabled
        )
    }

    suspend fun rename(
        societyId: Long,
        newName: String
    ) {
        require(newName.isNotBlank()) {
            "Society name cannot be empty"
        }

        val society = database.societyDao().findById(societyId)
            ?: throw IllegalArgumentException("Society not found")

        database.societyDao().update(
            society.copy(name = newName.trim(), cloudMetadataDirty = true)
        )
    }

    suspend fun markCloudMetadataSynced(
        societyId: Long
    ) {
        val society = database.societyDao().findById(societyId)
            ?: throw IllegalArgumentException("Society not found")
        database.societyDao().update(
            society.copy(cloudMetadataDirty = false)
        )
    }

    suspend fun applyCloudMetadata(
        societyId: Long,
        name: String
    ) {
        val society = database.societyDao().findById(societyId)
            ?: throw IllegalArgumentException("Society not found")
        database.societyDao().update(
            society.copy(name = name, cloudMetadataDirty = false)
        )
    }

    suspend fun delete(
        societyId: Long
    ) {
        database.deleteSocietyWithUsers(societyId)
    }
}
