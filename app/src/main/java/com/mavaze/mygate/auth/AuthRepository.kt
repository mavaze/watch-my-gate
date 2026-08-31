package com.mavaze.mygate.auth

import com.mavaze.mygate.data.local.AuthType
import com.mavaze.mygate.data.local.User
import com.mavaze.mygate.data.local.UserRole
import com.mavaze.mygate.data.repository.UserRepository

class AuthRepository(
    private val userRepository: UserRepository
) {
    suspend fun findUser(username: String) =
        userRepository.findByUsername(username)

    suspend fun login(
        username: String,
        password: String
    ): AuthResult {
        val user = userRepository.findByUsername(username)
            ?: return AuthResult.InvalidCredentials

        if (!user.enabled) return AuthResult.DisabledUser
        if (user.authType != AuthType.LOCAL) {
            return AuthResult.InvalidCredentials
        }

        val authenticated = userRepository.authenticate(username, password)
        return if (authenticated != null) {
            AuthResult.Success(authenticated)
        } else {
            AuthResult.InvalidCredentials
        }
    }

    suspend fun changePassword(
        user: User,
        newPassword: String
    ) {
        userRepository.changePassword(user, newPassword)
    }

    suspend fun setGoogleAuthorized(
        user: User,
        authorized: Boolean
    ): User {
        return userRepository.setGoogleAuthorized(
            user.id,
            authorized
        )
    }
}
