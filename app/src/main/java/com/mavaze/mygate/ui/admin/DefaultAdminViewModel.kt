package com.mavaze.mygate.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mavaze.mygate.data.local.Society
import com.mavaze.mygate.data.repository.SocietyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DefaultAdminUiState(
    val societies: List<Society> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class DefaultAdminViewModel(
    private val repository: SocietyRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow(DefaultAdminUiState())

    val state: StateFlow<DefaultAdminUiState> =
        _state.asStateFlow()

    fun loadSocieties() {
        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null
                )

            try {
                _state.value =
                    _state.value.copy(
                        societies = repository.getAll(),
                        loading = false
                    )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error =
                            e.message
                                ?: "Unable to load societies"
                    )
            }
        }
    }

    fun createSociety(
        name: String,
        adminEmail: String
    ) {
        viewModelScope.launch {
            runOperation {
                repository.createSociety(
                    name,
                    adminEmail
                )
            }
        }
    }

    fun setEnabled(
        societyId: Long,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            runOperation {
                repository.setEnabled(
                    societyId,
                    enabled
                )
            }
        }
    }

    fun renameSociety(
        societyId: Long,
        name: String
    ) {
        viewModelScope.launch {
            runOperation {
                repository.rename(
                    societyId,
                    name
                )
            }
        }
    }

    fun deleteSociety(
        societyId: Long,
        confirmationEmail: String
    ) {
        viewModelScope.launch {
            val society =
                repository.findById(societyId)

            if (society == null) {
                _state.value =
                    _state.value.copy(
                        error = "Society not found"
                    )
                return@launch
            }

            if (
                confirmationEmail.trim().lowercase() !=
                society.adminEmail.lowercase()
            ) {
                _state.value =
                    _state.value.copy(
                        error =
                            "Delete confirmation does not match the society Gmail address."
                    )
                return@launch
            }

            runOperation {
                repository.delete(societyId)
            }
        }
    }

    private suspend fun runOperation(
        operation: suspend () -> Unit
    ) {
        _state.value =
            _state.value.copy(
                loading = true,
                error = null
            )

        try {
            operation()

            _state.value =
                DefaultAdminUiState(
                    societies = repository.getAll()
                )
        } catch (e: Exception) {
            _state.value =
                _state.value.copy(
                    loading = false,
                    error =
                        e.message
                            ?: "Operation failed"
                )
        }
    }
}
