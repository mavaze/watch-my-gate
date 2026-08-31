package com.mavaze.mygate.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mavaze.mygate.auth.AuthRepository
import com.mavaze.mygate.auth.AuthResult
import com.mavaze.mygate.auth.GoogleAuthRepository
import com.mavaze.mygate.auth.GoogleDataSession
import com.mavaze.mygate.data.local.User
import com.mavaze.mygate.data.local.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val stage: LoginStage = LoginStage.USERNAME,
    val username: String = "",
    val user: User? = null,
    val busy: Boolean = false,
    val error: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val googleAuthRepository: GoogleAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun setUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            error = null
        )
    }

    fun continueWithUsername() {
        val username = _uiState.value.username.trim()

        if (username.isBlank()) {
            _uiState.value =
                _uiState.value.copy(error = "Enter username")
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(busy = true, error = null)

            val user = authRepository.findUser(username)

            if (user == null) {
                if (
                    username.lowercase().endsWith("@gmail.com")
                ) {
                    _uiState.value =
                        _uiState.value.copy(
                            user = null,
                            busy = false,
                            error = null,
                            stage = LoginStage.GOOGLE_AUTH
                        )
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            error = "User not found"
                        )
                }
                return@launch
            }

            if (!user.enabled) {
                _uiState.value =
                    _uiState.value.copy(
                        user = user,
                        busy = false,
                        error = "This user is disabled"
                    )
                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    user = user,
                    busy = false,
                    error = null
                )

            if (user.role == UserRole.SOCIETY_ADMIN) {
                _uiState.value =
                    _uiState.value.copy(
                        stage = LoginStage.GOOGLE_AUTH
                    )
            } else {
                _uiState.value =
                    _uiState.value.copy(
                        stage =
                            if (user.mustChangePassword)
                                LoginStage.CHANGE_PASSWORD
                            else
                                LoginStage.LOCAL_PASSWORD
                    )
            }
        }
    }

    fun googleLogin() {
        val user = _uiState.value.user
        val expectedEmail =
            user?.username
                ?: _uiState.value.username.trim()

        android.util.Log.d(
            "MyGateGoogle",
            "Expected email = '$expectedEmail'"
        )

//        android.util.Log.d(
//            "MyGateGoogle",
//            "Returned email = '${googleUser.email}'"
//        )

        if (!expectedEmail.lowercase().endsWith("@gmail.com")) {
            _uiState.value =
                _uiState.value.copy(
                    error = "Enter the registered society Gmail address"
                )
            return
        }

        if (
            user != null &&
            user.role != UserRole.SOCIETY_ADMIN
        ) {
            _uiState.value =
                _uiState.value.copy(
                    error = "This account does not use Google sign-in"
                )
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    busy = true,
                    error = null
                )

            googleAuthRepository.signIn(expectedEmail)
                .onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            stage =
                                LoginStage.GOOGLE_CONSENT,
                            busy = false,
                            error = null
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            error =
                                error.message
                                    ?: "Google sign-in failed"
                        )
                }
        }
    }

    fun adoptDiscoveredUser(user: User) {
        _uiState.value =
            _uiState.value.copy(
                user = user,
                username = user.username,
                error = null
            )
    }

    fun authorizationSucceeded(
        authorizedUser: User? = _uiState.value.user
    ) {
        val user = authorizedUser ?: return

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    busy = true,
                    error = null
                )

            try {
                val authorizedUser =
                    authRepository.setGoogleAuthorized(
                        user,
                        true
                    )

                _uiState.value =
                    _uiState.value.copy(
                        stage = LoginStage.LOGGED_IN,
                        user = authorizedUser,
                        busy = false,
                        error = null
                    )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        busy = false,
                        error =
                            e.message
                                ?: "Unable to save authorization state"
                    )
            }
        }
    }

    fun authorizationFailed(message: String) {
        _uiState.value =
            _uiState.value.copy(
                busy = false,
                error = message
            )
    }

    fun login(password: String) {
        val user = _uiState.value.user

        if (user == null) {
            _uiState.value =
                _uiState.value.copy(error = "User not found")
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(busy = true, error = null)

            when (
                val result =
                    authRepository.login(user.username, password)
            ) {
                is AuthResult.Success -> {
                    val authenticatedUser = result.user
                    _uiState.value =
                        _uiState.value.copy(
                            stage =
                                if (authenticatedUser.mustChangePassword)
                                    LoginStage.CHANGE_PASSWORD
                                else
                                    LoginStage.LOGGED_IN,
                            user = authenticatedUser,
                            busy = false,
                            error = null
                        )
                }

                AuthResult.InvalidCredentials ->
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            error = "Invalid username or password"
                        )

                AuthResult.DisabledUser ->
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            error = "This user is disabled"
                        )
            }
        }
    }

    fun logout() {
        GoogleDataSession.clear()
        _uiState.value = LoginUiState()
    }

    fun changePassword(
        newPassword: String,
        confirmPassword: String
    ) {
        if (newPassword.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    error = "Password cannot be empty"
                )
            return
        }

        if (newPassword != confirmPassword) {
            _uiState.value =
                _uiState.value.copy(
                    error = "Passwords do not match"
                )
            return
        }

        val user = _uiState.value.user

        if (user == null) {
            _uiState.value =
                _uiState.value.copy(error = "User not found")
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(busy = true, error = null)

            try {
                authRepository.changePassword(
                    user,
                    newPassword
                )

                val updatedUser =
                    authRepository.findUser(user.username)

                _uiState.value =
                    _uiState.value.copy(
                        stage = LoginStage.LOGGED_IN,
                        user = updatedUser ?: user,
                        busy = false,
                        error = null
                    )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        busy = false,
                        error =
                            e.message
                                ?: "Unable to change password"
                    )
            }
        }
    }
}
