package com.mavaze.mygate.ui.watchman

import android.Manifest
import android.provider.CallLog
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mavaze.mygate.data.local.GateContact
import com.mavaze.mygate.data.local.GateContactDao
import com.mavaze.mygate.telephony.CallEvent
import com.mavaze.mygate.telephony.MyGateCallController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Date

data class WatchmanAlias(
    val alias: String,
    val memberCount: Int
)

data class ActiveCall(
    val alias: String,
    val contactName: String,
    val attemptNumber: Int,
    val totalContacts: Int,
    val elapsedSeconds: Long = 0L
)

data class NativeCallHistoryEntry(
    val id: Long,
    val contactName: String,
    val alias: String,
    val incoming: Boolean,
    val details: String
)

data class CallHistoryEntry(
    val alias: String,
    val contactName: String,
    val attemptNumber: Int,
    val durationSeconds: Long,
    val reason: String,
    val connected: Boolean
)

data class WatchmanUiState(
    val aliases: List<WatchmanAlias> = emptyList(),
    val activeCall: ActiveCall? = null,
    val history: List<CallHistoryEntry> = emptyList(),
    val nativeCallHistory: List<NativeCallHistoryEntry> = emptyList(),
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
        MutableStateFlow(
            WatchmanUiState()
        )

    val state: StateFlow<WatchmanUiState> =
        _state.asStateFlow()

    private var callQueue:
            List<GateContact> =
        emptyList()

    private var currentIndex = 0

    private var mockMode = false

    private var mockJob: Job? = null

    private var timerJob: Job? = null

    private var sequenceId = 0L

    private var attemptStartedAt = 0L

    private var attemptConnected = false

    init {

        MyGateCallController.setListener(this)

        viewModelScope.launch {
            loadAliases()
            if (applicationContext.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                loadNativeCallHistory()
            }
        }
    }

    private suspend fun loadAliases() {

        Log.d(
            "MyGateWatchman",
            "Loading callable residents for societyId=$societyId"
        )

        val contacts =
            dao.getCallableForSociety(
                societyId
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

        _state.value =
            _state.value.copy(
                aliases = aliases,
                error = null
            )
    }

    /**
     * Returns the Watchman to the workflow home.
     *
     * If a call is active, it is terminated first.
     *
     * History is cleared because Home represents the beginning
     * of a new calling workflow.
     */
    fun returnHome() {

        mockMode = false

        mockJob?.cancel()
        mockJob = null

        timerJob?.cancel()
        timerJob = null

        MyGateCallController.reset()

        callQueue =
            emptyList()

        currentIndex = 0

        sequenceId++

        attemptStartedAt = 0L
        attemptConnected = false

        _state.value =
            _state.value.copy(
                activeCall = null,
                history = emptyList(),
                connected = false,
                busy = false,
                error = null
            )
    }

    fun refreshNativeCallHistory() {
        if (applicationContext.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch { loadNativeCallHistory() }
    }

    private suspend fun loadNativeCallHistory() {
        val contacts = dao.getCallableForSociety(societyId)
        val phoneToContact = mutableMapOf<String, GateContact>()
        contacts.forEach { contact ->
            try {
                val array = JSONArray(contact.phoneNumbersJson)
                for (i in 0 until array.length()) {
                    val number = normalizePhone(array.optString(i))
                    if (number.isNotBlank()) phoneToContact[number] = contact
                }
            } catch (_: Exception) { }
        }

        val result = mutableListOf<NativeCallHistoryEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
            CallLog.Calls.DATE, CallLog.Calls.DURATION
        )
        val sort = "${CallLog.Calls.DATE} DESC"
        try {
            applicationContext.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null, sort
            )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            while (cursor.moveToNext() && result.size < 100) {
                val type = cursor.getInt(typeCol)
                if (type != CallLog.Calls.INCOMING_TYPE && type != CallLog.Calls.OUTGOING_TYPE) continue
                val contact = phoneToContact[normalizePhone(cursor.getString(numberCol))] ?: continue
                val alias = contact.alias?.trim().orEmpty()
                if (alias.isBlank()) continue
                val incoming = type == CallLog.Calls.INCOMING_TYPE
                val timestamp = cursor.getLong(dateCol)
                val duration = cursor.getLong(durationCol)
                val time = DateFormat.format("dd MMM, hh:mm a", Date(timestamp)).toString()
                val durationText = formatDuration(duration)
                result += NativeCallHistoryEntry(
                    id = cursor.getLong(idCol),
                    contactName = contact.displayName.ifBlank { "Resident" },
                    alias = alias,
                    incoming = incoming,
                    details = "${if (incoming) "Incoming" else "Outgoing"} · $time · $durationText"
                )
            }
            }
        } catch (securityException: SecurityException) {
            Log.w("MyGateWatchman", "Call log permission is not available", securityException)
        }
        _state.value = _state.value.copy(nativeCallHistory = result)
    }

    private fun normalizePhone(value: String?): String {
        val digits = value.orEmpty().filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remaining = seconds % 60
        return "%02d:%02d".format(minutes, remaining)
    }

    fun mockCallAlias(
        alias: WatchmanAlias
    ) {

        if (
            _state.value.activeCall != null
        ) {
            return
        }

        mockJob?.cancel()
        timerJob?.cancel()

        viewModelScope.launch {

            val contacts =
                dao.getForAlias(
                    societyId,
                    alias.alias
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

            sequenceId++

            callQueue = contacts
            currentIndex = 0
            mockMode = true

            _state.value =
                _state.value.copy(
                    history = emptyList()
                )

            startAttemptTimer()
            showCurrentCall()

            mockJob =
                launch {

                    while (
                        mockMode &&
                        _state.value.activeCall != null
                    ) {

                        delay(
                            MOCK_CALL_DURATION_MS
                        )

                        if (
                            !mockMode ||
                            _state.value.activeCall == null
                        ) {
                            break
                        }

                        recordCurrentAttempt(
                            reason = "mock",
                            connected = false
                        )

                        currentIndex++

                        if (
                            currentIndex >=
                            callQueue.size
                        ) {

                            finishSequence()
                            break
                        }

                        startAttemptTimer()
                        showCurrentCall()
                    }
                }
        }
    }

    @RequiresPermission(
        Manifest.permission.CALL_PHONE
    )
    fun callAlias(
        alias: WatchmanAlias
    ) {

        if (
            _state.value.activeCall != null
        ) {
            return
        }

        viewModelScope.launch {

            val contacts =
                dao.getForAlias(
                    societyId,
                    alias.alias
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
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                _state.value =
                    _state.value.copy(
                        error =
                            "Phone-call permission is not granted."
                    )

                return@launch
            }

            sequenceId++

            mockMode = false

            mockJob?.cancel()
            mockJob = null

            timerJob?.cancel()

            callQueue = valid
            currentIndex = 0

            _state.value =
                _state.value.copy(
                    activeCall =
                        activeCall(
                            elapsedSeconds = 0L
                        ),
                    history = emptyList(),
                    connected = false,
                    busy = true,
                    error = null
                )

            startAttemptTimer()

            try {

                MyGateCallController.placeCall(
                    applicationContext,
                    phoneNumber =
                        firstPhoneNumber(
                            callQueue[
                                currentIndex
                            ].phoneNumbersJson
                        )!!
                )

            } catch (e: Exception) {

                stopAttemptTimer()

                _state.value =
                    _state.value.copy(
                        activeCall = null,
                        busy = false,
                        connected = false,
                        error =
                            e.message
                                ?: "Unable to start the call."
                    )

                callQueue =
                    emptyList()
            }
        }
    }

    fun cancelCall() {

        if (
            _state.value.activeCall == null
        ) {
            return
        }

        if (mockMode) {

            mockMode = false

            mockJob?.cancel()
            mockJob = null

            recordCurrentAttempt(
                reason = "cancelled",
                connected = attemptConnected
            )

            finishSequence()
            return
        }

        MyGateCallController.cancelCurrentCall()
    }

    fun skipCall() {

        if (
            _state.value.activeCall == null
        ) {
            return
        }

        if (mockMode) {

            recordCurrentAttempt(
                reason = "skipped",
                connected = attemptConnected
            )

            currentIndex++

            if (
                currentIndex >=
                callQueue.size
            ) {

                mockMode = false

                mockJob?.cancel()
                mockJob = null

                finishSequence()

            } else {

                startAttemptTimer()
                showCurrentCall()
            }

            return
        }

        MyGateCallController.skipCurrentCall()
    }

    override fun onCallEvent(
        event: CallEvent
    ) {

        viewModelScope.launch {

            when (event) {

                CallEvent.Connected -> {

                    if (
                        _state.value.activeCall == null
                    ) {
                        return@launch
                    }

                    attemptConnected = true

                    _state.value =
                        _state.value.copy(
                            connected = true,
                            error = null
                        )
                }

                is CallEvent.Ended -> {

                    if (
                        _state.value.activeCall == null
                    ) {
                        return@launch
                    }

                    recordCurrentAttempt(
                        reason = event.reason,
                        connected = attemptConnected
                    )

                    if (!event.advance) {

                        finishSequence()

                    } else {

                        advanceOrFinish(
                            event.reason
                        )
                    }
                }
            }
        }
    }

    private suspend fun advanceOrFinish(
        reason: String
    ) {

        currentIndex++

        if (
            currentIndex >=
            callQueue.size
        ) {

            finishSequence()
            return
        }

        startAttemptTimer()

        _state.value =
            _state.value.copy(
                activeCall =
                    activeCall(
                        elapsedSeconds = 0L
                    ),
                connected = false,
                busy = true,
                error = null
            )

        try {

            MyGateCallController.placeCall(
                applicationContext,
                firstPhoneNumber(
                    callQueue[
                        currentIndex
                    ].phoneNumbersJson
                )!!
            )

        } catch (e: Exception) {

            stopAttemptTimer()

            _state.value =
                _state.value.copy(
                    activeCall = null,
                    busy = false,
                    connected = false,
                    error =
                        "Unable to call " +
                                callQueue[
                                    currentIndex
                                ].displayName +
                                ": " +
                                (
                                        e.message
                                            ?: "unknown error"
                                        )
                )

            callQueue =
                emptyList()
        }
    }

    private fun showCurrentCall() {

        _state.value =
            _state.value.copy(
                activeCall =
                    activeCall(
                        elapsedSeconds = 0L
                    ),
                connected = false,
                busy = true,
                error = null
            )
    }

    private fun startAttemptTimer() {

        timerJob?.cancel()

        attemptStartedAt =
            SystemClock.elapsedRealtime()

        attemptConnected = false

        timerJob =
            viewModelScope.launch {

                while (isActive) {

                    val elapsed =
                        (
                                SystemClock.elapsedRealtime() -
                                        attemptStartedAt
                                ) / 1000L

                    val active =
                        _state.value.activeCall

                    if (active != null) {

                        _state.value =
                            _state.value.copy(
                                activeCall =
                                    active.copy(
                                        elapsedSeconds =
                                            elapsed
                                    )
                            )
                    }

                    delay(1000L)
                }
            }
    }

    private fun stopAttemptTimer() {

        timerJob?.cancel()
        timerJob = null
    }

    private fun recordCurrentAttempt(
        reason: String,
        connected: Boolean
    ) {

        val active =
            _state.value.activeCall
                ?: return

        val durationSeconds =
            if (
                attemptStartedAt == 0L
            ) {
                active.elapsedSeconds
            } else {
                (
                        SystemClock.elapsedRealtime() -
                                attemptStartedAt
                        ) / 1000L
            }

        val entry =
            CallHistoryEntry(
                alias = active.alias,
                contactName = active.contactName,
                attemptNumber =
                    active.attemptNumber,
                durationSeconds =
                    durationSeconds,
                reason = reason,
                connected = connected
            )

        _state.value =
            _state.value.copy(
                history =
                    _state.value.history +
                            entry
            )

        stopAttemptTimer()
    }

    private fun finishSequence() {

        mockMode = false

        mockJob?.cancel()
        mockJob = null

        stopAttemptTimer()

        callQueue =
            emptyList()

        currentIndex = 0

        attemptStartedAt = 0L
        attemptConnected = false

        _state.value =
            _state.value.copy(
                activeCall = null,
                connected = false,
                busy = false
            )
    }

    private fun activeCall(
        elapsedSeconds: Long
    ): ActiveCall =

        ActiveCall(
            alias =
                callQueue[
                    currentIndex
                ].alias!!
                    .trim(),

            contactName =
                callQueue[
                    currentIndex
                ].displayName
                    .ifBlank {
                        "Resident"
                    },

            attemptNumber =
                currentIndex + 1,

            totalContacts =
                callQueue.size,

            elapsedSeconds =
                elapsedSeconds
        )

    override fun onCleared() {

        mockMode = false

        mockJob?.cancel()
        timerJob?.cancel()

        MyGateCallController.setListener(
            null
        )

        super.onCleared()
    }

    private companion object {

        const val MOCK_CALL_DURATION_MS =
            3000L
    }

    private fun firstPhoneNumber(
        json: String
    ): String? {

        return try {

            val array =
                JSONArray(json)

            for (
            i in 0 until array.length()
            ) {

                val value =
                    array.optString(i)
                        .trim()

                if (
                    value.isNotBlank()
                ) {
                    return value
                }
            }

            null

        } catch (_: Exception) {
            null
        }
    }
}