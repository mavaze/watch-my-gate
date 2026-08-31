package com.mavaze.mygate.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import androidx.annotation.RequiresPermission

sealed interface CallEvent {
    data object Connected : CallEvent

    data class Ended(
        val advance: Boolean,
        val reason: String
    ) : CallEvent
}

object MyGateCallController {

    interface Listener {
        fun onCallEvent(event: CallEvent)
    }

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var listener: Listener? = null
    private var currentCall: Call? = null
    private var manualTermination: Termination? = null
    private var timeoutRunnable: Runnable? = null
    private var wasActive = false
    private var expectingOutgoingCall = false

    private enum class Termination {
        CANCEL,
        SKIP
    }

    private val callback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                state: Int
            ) {
                when (state) {
                    Call.STATE_ACTIVE -> {
                        wasActive = true
                        cancelTimeout()
                        listener?.onCallEvent(
                            CallEvent.Connected
                        )
                    }

                    Call.STATE_RINGING -> {
                        scheduleNoAnswerTimeout(call)
                    }

                    Call.STATE_DISCONNECTED -> {
                        cancelTimeout()

                        val manual =
                            manualTermination

                        val completed =
                            wasActive

                        manualTermination = null
                        currentCall = null
                        wasActive = false

                        if (manual == Termination.CANCEL) {
                            listener?.onCallEvent(
                                CallEvent.Ended(
                                    advance = false,
                                    reason = "cancelled"
                                )
                            )
                        } else if (manual == Termination.SKIP) {
                            listener?.onCallEvent(
                                CallEvent.Ended(
                                    advance = true,
                                    reason = "skipped"
                                )
                            )
                        } else {
                            listener?.onCallEvent(
                                CallEvent.Ended(
                                    advance = !completed,
                                    reason =
                                        if (completed)
                                            "completed"
                                        else
                                            disconnectReason(
                                                call.details.disconnectCause
                                            )
                                )
                            )
                        }
                    }
                }
            }
        }

    fun setListener(newListener: Listener?) {
        listener = newListener
    }

    fun onCallAdded(call: Call) {
        if (!expectingOutgoingCall) {
            return
        }

        expectingOutgoingCall = false

        currentCall?.let {
            try {
                it.unregisterCallback(callback)
            } catch (_: Exception) {
            }
        }

        currentCall = call
        wasActive = call.state == Call.STATE_ACTIVE

        try {
            call.registerCallback(callback)
        } catch (_: Exception) {
        }

        when (call.state) {
            Call.STATE_ACTIVE ->
                listener?.onCallEvent(CallEvent.Connected)

            Call.STATE_RINGING ->
                scheduleNoAnswerTimeout(call)
        }
    }

    fun onCallRemoved(call: Call) {
        if (currentCall === call) {
            cancelTimeout()
            currentCall = null
            expectingOutgoingCall = false
        }
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    fun placeCall(
        context: Context,
        phoneNumber: String
    ) {
        val telecom =
            context.getSystemService(
                TelecomManager::class.java
            ) ?: throw IllegalStateException(
                "Telecom service is unavailable."
            )

        if (
            context.checkSelfPermission(
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException(
                "CALL_PHONE permission is not granted."
            )
        }

        cancelTimeout()
        manualTermination = null
        expectingOutgoingCall = true

        val uri =
            Uri.parse(
                "tel:" +
                    Uri.encode(phoneNumber)
            )

        telecom.placeCall(
            uri,
            Bundle()
        )
    }

    fun cancelCurrentCall() {
        manualTermination = Termination.CANCEL
        disconnectCurrent()
    }

    fun skipCurrentCall() {
        manualTermination = Termination.SKIP
        disconnectCurrent()
    }

    private fun disconnectCurrent() {
        val call = currentCall

        if (call == null) {
            val manual = manualTermination
            manualTermination = null

            listener?.onCallEvent(
                CallEvent.Ended(
                    advance =
                        manual == Termination.SKIP,
                    reason =
                        if (manual == Termination.SKIP)
                            "skipped"
                        else
                            "cancelled"
                )
            )
            return
        }

        try {
            call.disconnect()
        } catch (e: Exception) {
            val manual = manualTermination
            manualTermination = null

            listener?.onCallEvent(
                CallEvent.Ended(
                    advance =
                        manual == Termination.SKIP,
                    reason =
                        "Unable to disconnect call: " +
                            (e.message ?: "unknown error")
                )
            )
        }
    }

    private fun scheduleNoAnswerTimeout(
        call: Call
    ) {
        cancelTimeout()

        val runnable =
            Runnable {
                if (currentCall === call) {
                    manualTermination = null

                    try {
                        call.disconnect()
                    } catch (_: Exception) {
                    }
                }
            }

        timeoutRunnable = runnable

        mainHandler.postDelayed(
            runnable,
            NO_ANSWER_TIMEOUT_MS
        )
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
        }

        timeoutRunnable = null
    }

    private fun disconnectReason(
        cause: DisconnectCause
    ): String =
        when (cause.code) {
            DisconnectCause.BUSY ->
                "busy"

            DisconnectCause.REJECTED ->
                "rejected"

            DisconnectCause.ERROR ->
                "error"

            DisconnectCause.LOCAL ->
                "local"

            DisconnectCause.REMOTE ->
                "remote"

            DisconnectCause.CANCELED ->
                "cancelled"

            else ->
                cause.label?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: "ended"
        }

    private const val NO_ANSWER_TIMEOUT_MS =
        25_000L
}
