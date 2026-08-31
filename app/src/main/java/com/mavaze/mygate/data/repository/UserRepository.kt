package com.mavaze.mygate.data.repository

import com.mavaze.mygate.auth.PasswordHasher
import com.mavaze.mygate.data.local.AuthType
import com.mavaze.mygate.data.local.User
import com.mavaze.mygate.data.local.UserDao
import com.mavaze.mygate.data.local.UserRole

class UserRepository(
    private val userDao: UserDao
) {
    suspend fun findByUsername(username: String): User? =
        userDao.findByUsername(username.trim())

    suspend fun authenticate(
        username: String,
        password: String
    ): User? {
        val user = findByUsername(username) ?: return null
        if (!user.enabled || user.authType != AuthType.LOCAL) return null

        val storedHash = user.passwordHash ?: return null
        if (!PasswordHasher.verify(password, storedHash)) return null

        return user
    }

    suspend fun createLocalUser(
        username: String,
        displayName: String,
        societyId: Long?,
        role: UserRole,
        password: String,
        mustChangePassword: Boolean = true
    ): Long {
        require(username.isNotBlank())
        require(displayName.isNotBlank())
        require(password.isNotBlank())

        return userDao.insert(
            User(
                username = username.trim(),
                displayName = displayName.trim(),
                societyId = societyId,
                role = role,
                authType = AuthType.LOCAL,
                passwordHash = PasswordHasher.hash(password),
                mustChangePassword = mustChangePassword,
                enabled = true
            )
        )
    }

    suspend fun createWatchman(
        societyId: Long,
        username: String,
        displayName: String,
        temporaryPassword: String
    ): Long {
        require(username.isNotBlank()) { "Username cannot be empty" }
        require(displayName.isNotBlank()) { "Display name cannot be empty" }
        require(temporaryPassword.isNotBlank()) {
            "Temporary password cannot be empty"
        }

        if (userDao.findByUsername(username.trim()) != null) {
            throw IllegalArgumentException("Username is already in use")
        }

        return createLocalUser(
            username = username,
            displayName = displayName,
            societyId = societyId,
            role = UserRole.WATCHMAN,
            password = temporaryPassword,
            mustChangePassword = true
        )
    }

    suspend fun changePassword(
        user: User,
        newPassword: String
    ) {
        require(newPassword.isNotBlank()) {
            "Password cannot be empty"
        }

        userDao.update(
            user.copy(
                passwordHash = PasswordHasher.hash(newPassword),
                mustChangePassword = false
            )
        )
    }

    suspend fun setGoogleAuthorized(
        userId: Long,
        authorized: Boolean
    ): User {
        userDao.setGoogleAuthorized(userId, authorized)
        return userDao.findById(userId)
            ?: throw IllegalStateException("User no longer exists")
    }
}
