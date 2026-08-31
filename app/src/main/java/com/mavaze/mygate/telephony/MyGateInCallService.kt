package com.mavaze.mygate.telephony

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.mavaze.mygate.MyGateApplication
import com.mavaze.mygate.IncomingCallActivity
import com.mavaze.mygate.data.local.GateContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray

class MyGateInCallService : InCallService() {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val callback =
        object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                when (state) {
                    Call.STATE_ACTIVE ->
                        IncomingCallManager.updateActive(true)

                    Call.STATE_DISCONNECTED ->
                        IncomingCallManager.clear(call)
                }
            }
        }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val outgoing =
            call.details.callDirection == Call.Details.DIRECTION_OUTGOING
        val incoming =
            call.details.callDirection == Call.Details.DIRECTION_INCOMING

        if (!outgoing && !incoming) {
            return
        }

        if (outgoing) {
            MyGateCallController.onCallAdded(call)
        }

        if (incoming) {
            try {
                call.registerCallback(callback)
            } catch (_: Exception) {
            }

            scope.launch {
                val contact = findContact(call)
                val alias = contact?.alias?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Unknown resident"
                val name = contact?.displayName?.trim()?.takeIf { it.isNotBlank() }

                IncomingCallManager.setCall(
                    call = call,
                    alias = alias,
                    displayName = name,
                    incoming = true,
                    active = call.state == Call.STATE_ACTIVE
                )

                val intent = Intent(
                    this@MyGateInCallService,
                    IncomingCallActivity::class.java
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }

                startActivity(intent)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        try {
            call.unregisterCallback(callback)
        } catch (_: Exception) {
        }
        MyGateCallController.onCallRemoved(call)
        IncomingCallManager.clear(call)
        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        scope.cancel()
        IncomingCallManager.clear()
        MyGateCallController.setListener(null)
        super.onDestroy()
    }

    private suspend fun findContact(call: Call): GateContact? {
        val application = application as MyGateApplication
        val config = application.database.appConfigDao().get()
        val societyId = config?.configuredSocietyId ?: return null
        val handle = call.details.handle?.schemeSpecificPart ?: return null
        val incoming = normalizeNumber(handle)

        return application.database
            .gateContactDao()
            .getCallableForSociety(societyId)
            .firstOrNull { contact ->
                try {
                    val numbers = JSONArray(contact.phoneNumbersJson)
                    (0 until numbers.length()).any { index ->
                        normalizeNumber(numbers.optString(index)) == incoming
                    }
                } catch (_: Exception) {
                    false
                }
            }
    }

    private fun normalizeNumber(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length > 10) digits.takeLast(10) else digits
    }
}
