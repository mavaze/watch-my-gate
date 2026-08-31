package com.mavaze.mygate.ui.watchman

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.mavaze.mygate.data.local.GateContact
import com.mavaze.mygate.data.local.GateContactDao
import com.mavaze.mygate.telephony.CallEvent
import com.mavaze.mygate.telephony.MyGateCallController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

data class WatchmanAlias(
    val alias: String,
    val memberCount: Int
)

data class ActiveCall(
    val alias: String,
    val contactName: String,
    val attemptNumber: Int,
    val totalContacts: Int
)

data class WatchmanUiState(
    val aliases: List<WatchmanAlias> = emptyList(),
    val activeCall: ActiveCall? = null,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null
)

class WatchmanViewModel(
    private val societyId: Long,
    private val dao: GateContactDao,
    private val applicationContext: Context
) : ViewModel(), MyGateCallController.Listener {

    private val _state =
        MutableStateFlow(WatchmanUiState())

    val state: StateFlow<WatchmanUiState> =
        _state.asStateFlow()

    private var callQueue: List<GateContact> = emptyList()
    private var currentIndex = 0
    private var mockMode = false
    private var mockJob: kotlinx.coroutines.Job? = null

    init {
        MyGateCallController.setListener(this)

        viewModelScope.launch {
            loadAliases()
        }
    }

    private suspend fun loadAliases() {
        Log.d("MyGateWatchman", "Loading callable residents for societyId=$societyId")

        val contacts =
            dao.getCallableForSociety(societyId)

        Log.d(
            "MyGateWatchman",
            "DB callable contacts=${contacts.size}: " +
                contacts.joinToString {
                    "${it.displayName} [${it.alias}, p=${it.priority}, phones=${firstPhoneNumber(it.phoneNumbersJson) != null}]"
                }
        )

        val aliases =
            contacts
                .groupBy {
                    it.alias!!.trim()
                }
                .map { (alias, members) ->
                    WatchmanAlias(
                        alias = alias,
                        memberCount = members.size
                    )
                }
                .sortedBy {
                    it.alias.lowercase()
                }

        Log.d(
            "MyGateWatchman",
            "DB aliases=${aliases.size}: ${aliases.joinToString { "${it.alias}(${it.memberCount})" }}"
        )

        _state.value =
            _state.value.copy(
                aliases = aliases,
                error = null
            )
    }

    fun mockCallAlias(alias: String) {
        if (_state.value.activeCall != null) {
            return
        }

        mockJob?.cancel()

        viewModelScope.launch {
            val contacts =
                dao.getForAlias(
                    societyId,
                    alias
                ).filter {
                    it.phoneNumbersJson
                        .let(::firstPhoneNumber)
                        ?.isNotBlank() == true
                }

            if (contacts.isEmpty()) {
                _state.value =
                    _state.value.copy(
                        error =
                            "No callable residents are configured for $alias."
                    )
                return@launch
            }

            callQueue = contacts
            currentIndex = 0
            mockMode = true

            showCurrentCall()

            mockJob = launch {
                while (mockMode && _state.value.activeCall != null) {
                    delay(MOCK_CALL_DURATION_MS)

                    if (!mockMode || _state.value.activeCall == null) {
                        break
                    }

                    currentIndex++
                    if (currentIndex >= callQueue.size) {
                        finishSequence()
                        break
                    }

                    showCurrentCall()
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    fun callAlias(alias: String) {
        if (_state.value.activeCall != null) {
            return
        }

        viewModelScope.launch {
            val contacts =
                dao.getForAlias(
                    societyId,
                    alias
                )

            val valid =
                contacts.filter {
                    it.phoneNumbersJson
                        .let(::firstPhoneNumber)
                        ?.isNotBlank() == true
                }

            if (valid.isEmpty()) {
                _state.value =
                    _state.value.copy(
                        error =
                            "No callable residents are configured for $alias."
                    )
                return@launch
            }

            if (
                applicationContext.checkSelfPermission(
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                _state.value =
                    _state.value.copy(
                        error =
                            "Phone-call permission is not granted."
                    )
                return@launch
            }

            mockMode = false
            mockJob?.cancel()
            mockJob = null
            callQueue = valid
            currentIndex = 0
            _state.value =
                _state.value.copy(
                    activeCall = activeCall(),
                    connected = false,
                    busy = true,
                    error = null
                )

            try {
                MyGateCallController.placeCall(
                    applicationContext,
                    phoneNumber =
                        firstPhoneNumber(
                            callQueue[currentIndex]
                                .phoneNumbersJson
                        )!!
                )
            } catch (e: Exception) {
                _state.value =
                    _state.value.copy(
                        activeCall = null,
                        busy = false,
                        error =
                            e.message
                                ?: "Unable to start the call."
                    )
                callQueue = emptyList()
            }
        }
    }

    fun cancelCall() {
        if (_state.value.activeCall == null) {
            return
        }

        if (mockMode) {
            mockMode = false
            mockJob?.cancel()
            mockJob = null
            finishSequence()
            return
        }

        MyGateCallController.cancelCurrentCall()
    }

    fun skipCall() {
        if (_state.value.activeCall == null) {
            return
        }

        if (mockMode) {
            currentIndex++
            if (currentIndex >= callQueue.size) {
                mockMode = false
                mockJob?.cancel()
                mockJob = null
                finishSequence()
            } else {
                showCurrentCall()
            }
            return
        }

        MyGateCallController.skipCurrentCall()
    }

    override fun onCallEvent(event: CallEvent) {
        viewModelScope.launch {
            when (event) {
                CallEvent.Connected -> {
                    _state.value =
                        _state.value.copy(
                            connected = true,
                            error = null
                        )
                }

                is CallEvent.Ended -> {
                    if (!event.advance) {
                        finishSequence()
                    } else {
                        advanceOrFinish(event.reason)
                    }
                }
            }
        }
    }

    private suspend fun advanceOrFinish(
        reason: String
    ) {
        currentIndex++

        if (currentIndex >= callQueue.size) {
            finishSequence()
            return
        }

        val next =
            callQueue[currentIndex]

        _state.value =
            _state.value.copy(
                activeCall = activeCall(),
                connected = false,
                busy = true,
                error = null
            )

        try {
            MyGateCallController.placeCall(
                applicationContext,
                firstPhoneNumber(
                    next.phoneNumbersJson
                )!!
            )
        } catch (e: Exception) {
            _state.value =
                _state.value.copy(
                    activeCall = null,
                    busy = false,
                    error =
                        "Unable to call ${next.displayName}: " +
                            (e.message ?: "unknown error")
                )
            callQueue = emptyList()
        }
    }

    private fun showCurrentCall() {
        _state.value =
            _state.value.copy(
                activeCall = activeCall(),
                connected = false,
                busy = true,
                error = null
            )
    }

    private fun finishSequence() {
        mockMode = false
        mockJob?.cancel()
        mockJob = null
        callQueue = emptyList()
        currentIndex = 0

        _state.value =
            _state.value.copy(
                activeCall = null,
                connected = false,
                busy = false
            )
    }

    private fun activeCall(): ActiveCall =
        ActiveCall(
            alias =
                callQueue[currentIndex]
                    .alias!!
                    .trim(),
            contactName =
                callQueue[currentIndex]
                    .displayName
                    .ifBlank { "Resident" },
            attemptNumber =
                currentIndex + 1,
            totalContacts =
                callQueue.size
        )

    override fun onCleared() {
        mockMode = false
        mockJob?.cancel()
        MyGateCallController.setListener(null)
        super.onCleared()
    }

    private companion object {
        const val MOCK_CALL_DURATION_MS = 3000L
    }

    private fun firstPhoneNumber(
        json: String
    ): String? {
        return try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val value =
                    array.optString(i)
                        .trim()

                if (value.isNotBlank()) {
                    return value
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }


}
