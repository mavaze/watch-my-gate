package com.mavaze.mygate.telephony

import android.telecom.Call
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IncomingCallManager {

    data class CallUiInfo(
        val alias: String,
        val displayName: String?,
        val incoming: Boolean,
        val active: Boolean
    )

    private val _state =
        MutableStateFlow<CallUiInfo?>(null)

    val state: StateFlow<CallUiInfo?> =
        _state.asStateFlow()

    private var currentCall: Call? = null

    fun setCall(
        call: Call,
        alias: String,
        displayName: String?,
        incoming: Boolean,
        active: Boolean
    ) {
        currentCall = call
        _state.value = CallUiInfo(
            alias = alias,
            displayName = displayName,
            incoming = incoming,
            active = active
        )
    }

    fun updateActive(active: Boolean) {
        _state.value = _state.value?.copy(active = active)
    }

    fun answer() {
        currentCall?.answer(
            VideoProfile.STATE_AUDIO_ONLY
        )
    }

    fun reject() {
        currentCall?.disconnect()
        clear()
    }

    fun clear(call: Call? = null) {
        if (call == null || currentCall === call) {
            currentCall = null
            _state.value = null
        }
    }
}
