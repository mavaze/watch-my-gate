package com.mavaze.mygate.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mavaze.mygate.data.local.User
import com.mavaze.mygate.data.repository.SocietyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SocietyAdminUiState(
    val societyName: String = "",
    val adminEmail: String = "",
    val watchmen: List<User> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class SocietyAdminViewModel(
    private val repository: SocietyRepository,
    private val societyId: Long
) : ViewModel() {

    private val _state =
        MutableStateFlow(SocietyAdminUiState())

    val state: StateFlow<SocietyAdminUiState> =
        _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null
                )

            try {
                val society =
                    repository.findById(societyId)
                        ?: throw IllegalArgumentException(
                            "Society not found"
                        )

                _state.value =
                    SocietyAdminUiState(
                        societyName = society.name,
                        adminEmail = society.adminEmail,
                        watchmen =
                            repository.getWatchmen(societyId)
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error =
                            e.message
                                ?: "Unable to load society"
                    )
            }
        }
    }

    fun setError(message: String) {
        _state.value =
            _state.value.copy(error = message)
    }

    fun renameSociety(name: String) {
        viewModelScope.launch {
            try {
                repository.rename(
                    societyId,
                    name
                )
                load()
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        error =
                            e.message
                                ?: "Unable to rename society"
                    )
            }
        }
    }

    fun createWatchman(
        username: String,
        displayName: String,
        temporaryPassword: String
    ) {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null
                )

            try {
                repository.createWatchman(
                    societyId,
                    username,
                    displayName,
                    temporaryPassword
                )

                _state.value =
                    _state.value.copy(
                        watchmen =
                            repository.getWatchmen(societyId),
                        loading = false
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error =
                            e.message
                                ?: "Unable to create watchman"
                    )
            }
        }
    }
}
