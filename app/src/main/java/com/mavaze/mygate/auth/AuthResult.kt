package com.mavaze.mygate.auth

import com.mavaze.mygate.data.local.User

sealed interface AuthResult {

    data class Success(
        val user: User
    ) : AuthResult

    data object InvalidCredentials : AuthResult

    data object DisabledUser : AuthResult
}