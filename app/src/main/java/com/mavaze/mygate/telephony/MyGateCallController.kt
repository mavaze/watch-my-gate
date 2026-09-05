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

    /**
     * The Call currently owned by this controller.
     */
    private var currentCall: Call? = null

    /**
     * Manual action requested by the Watchman.
     */
    private var manualTermination: Termination? = null

    /**
     * Timeout used ONLY while waiting for the recipient to answer.
     *
     * There is intentionally no timeout after Telecom reports
     * STATE_DISCONNECTED.
     */
    private var timeoutRunnable: Runnable? = null

    /**
     * True once Telecom reports STATE_ACTIVE.
     */
    private var wasActive = false

    /**
     * True after placeCall() and before Telecom delivers onCallAdded().
     */
    private var expectingOutgoingCall = false

    private var expectedPhoneNumber: String? = null

    /**
     * Protects against STATE_DISCONNECTED and onCallRemoved both
     * producing terminal events for the same Call.
     */
    private var disconnectHandled = false

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
                /*
                 * Ignore callbacks from an old Call.
                 */
                if (currentCall !== call) {
                    return
                }

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
                        handleDisconnected(call)
                    }
                }
            }
        }

    fun setListener(
        newListener: Listener?
    ) {
        listener = newListener
    }

    /**
     * Called by the Telecom connection/service when a new Call appears.
     */
    fun onCallAdded(
        call: Call
    ) {

        if (!expectingOutgoingCall) {
            return
        }

        val expected =
            expectedPhoneNumber

        val actual =
            call.details.handle
                ?.schemeSpecificPart

        /*
         * Do not accidentally take ownership of a different call.
         */
        if (
            expected != null &&
            actual != null &&
            normalizeNumber(expected) !=
            normalizeNumber(actual)
        ) {
            return
        }

        expectingOutgoingCall = false
        expectedPhoneNumber = null

        /*
         * Defensive cleanup of an old Call.
         */
        currentCall?.let { oldCall ->

            try {
                oldCall.unregisterCallback(
                    callback
                )
            } catch (_: Exception) {
            }
        }

        currentCall = call

        wasActive =
            call.state == Call.STATE_ACTIVE

        disconnectHandled = false

        try {
            call.registerCallback(
                callback
            )
        } catch (_: Exception) {
        }

        /*
         * Telecom may already have changed the state before callback
         * registration.
         */
        when (call.state) {

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
                handleDisconnected(call)
            }
        }
    }

    /**
     * Called when Telecom removes a Call.
     *
     * Normally STATE_DISCONNECTED is received first. This method is
     * retained as a fallback for implementations that remove the Call
     * without delivering that callback.
     */
    fun onCallRemoved(
        call: Call
    ) {

        if (currentCall !== call) {
            return
        }

        /*
         * STATE_DISCONNECTED already handled this Call.
         */
        if (disconnectHandled) {
            cleanupCall(call)
            return
        }

        cancelTimeout()

        val manual =
            manualTermination

        val completed =
            wasActive

        disconnectHandled = true

        manualTermination = null
        currentCall = null
        expectingOutgoingCall = false
        expectedPhoneNumber = null
        wasActive = false

        val event =
            when (manual) {

                Termination.CANCEL ->
                    CallEvent.Ended(
                        advance = false,
                        reason = "cancelled"
                    )

                Termination.SKIP ->
                    CallEvent.Ended(
                        advance = true,
                        reason = "skipped"
                    )

                null ->
                    CallEvent.Ended(
                        advance = !completed,
                        reason =
                            if (completed) {
                                "completed"
                            } else {
                                "call_removed"
                            }
                    )
            }

        listener?.onCallEvent(event)

        try {
            call.unregisterCallback(
                callback
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Completely resets controller state.
     *
     * Used when leaving the calling workflow or logging out.
     */
    fun reset() {

        cancelTimeout()

        val call =
            currentCall

        currentCall = null
        manualTermination = null
        expectingOutgoingCall = false
        expectedPhoneNumber = null
        wasActive = false
        disconnectHandled = true

        if (call != null) {

            try {
                call.unregisterCallback(
                    callback
                )
            } catch (_: Exception) {
            }

            try {
                call.disconnect()
            } catch (_: Exception) {
            }
        }
    }

//    @RequiresPermission(Manifest.permission.CALL_PHONE)
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

        /*
         * A new call is starting.
         *
         * Fully detach the previous Call before asking Telecom for
         * another one. This prevents old callbacks from corrupting
         * the state of the new call.
         */
        cancelTimeout()

        currentCall?.let { oldCall ->

            try {
                oldCall.unregisterCallback(
                    callback
                )
            } catch (_: Exception) {
            }
        }

        currentCall = null
        manualTermination = null
        wasActive = false
        disconnectHandled = false

        expectingOutgoingCall = true
        expectedPhoneNumber = phoneNumber

        val uri =
            Uri.parse(
                "tel:" +
                        Uri.encode(phoneNumber)
            )

        try {

            telecom.placeCall(
                uri,
                Bundle()
            )

        } catch (e: Exception) {

            /*
             * Telecom rejected the request before creating a Call.
             */
            expectingOutgoingCall = false
            expectedPhoneNumber = null
            disconnectHandled = true

            throw e
        }
    }

    /**
     * Watchman pressed Cancel.
     *
     * Cancel terminates the sequence without advancing.
     */
    fun cancelCurrentCall() {

        if (manualTermination != null) {
            return
        }

        manualTermination =
            Termination.CANCEL

        disconnectCurrent()
    }

    /**
     * Watchman pressed Skip.
     *
     * Skip advances to the next resident.
     */
    fun skipCurrentCall() {

        if (manualTermination != null) {
            return
        }

        manualTermination =
            Termination.SKIP

        disconnectCurrent()
    }

    /**
     * Disconnect the current Telecom Call.
     *
     * If Telecom has already removed it, complete the manual operation
     * immediately so the UI cannot become stuck.
     */
    private fun disconnectCurrent() {

        val call =
            currentCall

        if (call == null) {

            val manual =
                manualTermination

            manualTermination = null
            disconnectHandled = true

            listener?.onCallEvent(
                CallEvent.Ended(
                    advance =
                        manual ==
                                Termination.SKIP,
                    reason =
                        if (
                            manual ==
                            Termination.SKIP
                        ) {
                            "skipped"
                        } else {
                            "cancelled"
                        }
                )
            )

            return
        }

        try {

            call.disconnect()

        } catch (_: Exception) {

            /*
             * Do not leave the Watchman screen blocked if the Telecom
             * implementation refuses disconnect().
             */
            val manual =
                manualTermination

            manualTermination = null
            currentCall = null
            expectingOutgoingCall = false
            expectedPhoneNumber = null
            wasActive = false
            disconnectHandled = true

            cancelTimeout()

            try {
                call.unregisterCallback(
                    callback
                )
            } catch (_: Exception) {
            }

            listener?.onCallEvent(
                CallEvent.Ended(
                    advance =
                        manual ==
                                Termination.SKIP,
                    reason =
                        if (
                            manual ==
                            Termination.SKIP
                        ) {
                            "skipped"
                        } else {
                            "cancelled"
                        }
                )
            )
        }
    }

    /**
     * Gives the recipient a limited amount of time to answer.
     *
     * IMPORTANT:
     *
     * This timeout only applies while Telecom reports RINGING.
     *
     * Once Telecom reports ACTIVE, BUSY, ERROR, REJECTED, etc.,
     * there is no additional application delay.
     */
    private fun scheduleNoAnswerTimeout(
        call: Call
    ) {

        cancelTimeout()

        val runnable =
            Runnable {

                if (
                    currentCall !== call ||
                    disconnectHandled
                ) {
                    return@Runnable
                }

                if (
                    call.state !=
                    Call.STATE_RINGING
                ) {
                    return@Runnable
                }

                /*
                 * Automatic timeout is not a manual Skip.
                 */
                manualTermination = null

                try {
                    call.disconnect()
                } catch (_: Exception) {
                }
            }

        timeoutRunnable = runnable

        mainHandler.postDelayed(
            runnable,
            NO_ANSWER_TIMEOUT_MS
        )
    }

    /**
     * Handles the authoritative Telecom disconnect event.
     *
     * The key rule here is:
     *
     *     STATE_DISCONNECTED -> notify ViewModel immediately.
     *
     * There is deliberately no BUSY_TIMEOUT or ERROR_TIMEOUT.
     */
    private fun handleDisconnected(
        call: Call
    ) {

        if (currentCall !== call) {
            return
        }

        if (disconnectHandled) {
            return
        }

        disconnectHandled = true

        cancelTimeout()

        val manual =
            manualTermination

        val completed =
            wasActive

        val cause =
            call.details.disconnectCause

        /*
         * Determine the event BEFORE clearing state.
         */
        val event =
            when {

                manual ==
                        Termination.CANCEL -> {

                    CallEvent.Ended(
                        advance = false,
                        reason = "cancelled"
                    )
                }

                manual ==
                        Termination.SKIP -> {

                    CallEvent.Ended(
                        advance = true,
                        reason = "skipped"
                    )
                }

                completed -> {

                    CallEvent.Ended(
                        advance = false,
                        reason = "completed"
                    )
                }

                /*
                 * Your device/carrier reports both:
                 *
                 *   recipient rejected
                 *   recipient already on another call
                 *
                 * as Telecom BUSY / SIP 486.
                 *
                 * Therefore both correctly become "Busy".
                 */
                cause.code ==
                        DisconnectCause.BUSY -> {

                    CallEvent.Ended(
                        advance = true,
                        reason = "busy"
                    )
                }

                /*
                 * ERROR is further classified using the public
                 * DisconnectCause.reason.
                 *
                 * Your logs:
                 *
                 * ERROR_UNSPECIFIED + SIP 480
                 *     -> recipient did not answer / temporarily
                 *        unavailable
                 *
                 * NUMBER_UNREACHABLE + SIP 484
                 *     -> invalid/unreachable number
                 */
                cause.code ==
                        DisconnectCause.ERROR -> {

                    CallEvent.Ended(
                        advance = true,
                        reason =
                            disconnectReason(
                                cause
                            )
                    )
                }

                /*
                 * Any other pre-answer termination advances.
                 */
                else -> {

                    CallEvent.Ended(
                        advance = true,
                        reason =
                            disconnectReason(
                                cause
                            )
                    )
                }
            }

        /*
         * CRITICAL:
         *
         * Clear all old-call state BEFORE notifying the ViewModel.
         *
         * The ViewModel can therefore immediately invoke placeCall()
         * for the next resident without racing with old state cleanup.
         */
        manualTermination = null
        currentCall = null
        expectingOutgoingCall = false
        expectedPhoneNumber = null
        wasActive = false

        try {
            call.unregisterCallback(
                callback
            )
        } catch (_: Exception) {
        }

        /*
         * The next resident is now allowed to start immediately.
         */
        listener?.onCallEvent(event)
    }

    /**
     * Defensive cleanup after STATE_DISCONNECTED has already been
     * processed.
     */
    private fun cleanupCall(
        call: Call
    ) {

        cancelTimeout()

        if (currentCall === call) {
            currentCall = null
        }

        manualTermination = null
        expectingOutgoingCall = false
        expectedPhoneNumber = null
        wasActive = false
        disconnectHandled = true

        try {
            call.unregisterCallback(
                callback
            )
        } catch (_: Exception) {
        }
    }

    private fun cancelTimeout() {

        timeoutRunnable?.let {
            mainHandler.removeCallbacks(
                it
            )
        }

        timeoutRunnable = null
    }

    private fun normalizeNumber(
        value: String
    ): String {

        val digits =
            value.filter(
                Char::isDigit
            )

        return if (
            digits.length > 10
        ) {
            digits.takeLast(10)
        } else {
            digits
        }
    }

    /**
     * Converts the public DisconnectCause into the application's
     * call-sequence reason.
     *
     * We intentionally do NOT use ImsReasonInfo here because
     * DisconnectCause does not expose a public imReasonInfo property.
     */
    private fun disconnectReason(
        cause: DisconnectCause
    ): String {

        return when {

            cause.code ==
                    DisconnectCause.BUSY -> {
                "busy"
            }

            cause.code ==
                    DisconnectCause.REJECTED -> {
                "rejected"
            }

            cause.code ==
                    DisconnectCause.ERROR -> {

                when {

                    "NUMBER_UNREACHABLE".equals(
                        cause.reason,
                        ignoreCase = true
                    ) -> {
                        "unreachable"
                    }

                    "ERROR_UNSPECIFIED".equals(
                        cause.reason,
                        ignoreCase = true
                    ) -> {
                        "no_answer"
                    }

                    else -> {
                        "error"
                    }
                }
            }

            cause.code ==
                    DisconnectCause.LOCAL -> {
                "local"
            }

            cause.code ==
                    DisconnectCause.REMOTE -> {
                "remote"
            }

            cause.code ==
                    DisconnectCause.CANCELED -> {
                "cancelled"
            }

            else -> {

                cause.label
                    ?.toString()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "ended"
            }
        }
    }

    /*
     * ============================================================
     * CALL TIMING
     * ============================================================
     *
     * Only the no-answer waiting period is controlled here.
     *
     * Once Telecom reports STATE_DISCONNECTED:
     *
     *     BUSY       -> immediately advance
     *     ERROR      -> immediately advance
     *     REJECTED   -> immediately advance
     *
     * There is no artificial post-disconnect delay.
     *
     * 1 second is intentionally retained for the current ringing
     * timeout configuration.
     */
    private const val NO_ANSWER_TIMEOUT_MS = 1_000L
}